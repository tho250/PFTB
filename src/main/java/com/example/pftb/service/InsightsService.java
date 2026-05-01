package com.example.pftb.service;

import com.example.pftb.dto.InsightsSummaryResponse;
import com.example.pftb.dto.TopExpenseCategoryDto;
import com.example.pftb.entity.PaymentObligation;
import com.example.pftb.entity.PaymentObligationStatus;
import com.example.pftb.entity.PaymentObligationType;
import com.example.pftb.entity.Transaction;
import com.example.pftb.entity.TransactionType;
import com.example.pftb.entity.User;
import com.example.pftb.repository.PaymentObligationRepository;
import com.example.pftb.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class InsightsService {
    private final TransactionRepository transactionRepository;
    private final PaymentObligationRepository paymentObligationRepository;
    private final UserContextService userContextService;

    public InsightsService(TransactionRepository transactionRepository,
                           PaymentObligationRepository paymentObligationRepository,
                           UserContextService userContextService) {
        this.transactionRepository = transactionRepository;
        this.paymentObligationRepository = paymentObligationRepository;
        this.userContextService = userContextService;
    }

    @Transactional(readOnly = true)
    public InsightsSummaryResponse getSummary(String username) {
        User user = userContextService.getUserByUsername(username);
        List<Transaction> transactions = transactionRepository.findByUserId(user.getId());
        List<PaymentObligation> obligations = paymentObligationRepository.findByUserIdOrderByDueDateAsc(user.getId());

        YearMonth currentMonth = YearMonth.now();
        BigDecimal currentMonthIncome = sumTransactions(transactions, TransactionType.INCOME, currentMonth);
        BigDecimal currentMonthExpense = sumTransactions(transactions, TransactionType.EXPENSE, currentMonth);
        BigDecimal currentMonthNet = currentMonthIncome.subtract(currentMonthExpense);

        BigDecimal averageMonthlyIncome = averageMonthly(transactions, TransactionType.INCOME);
        BigDecimal averageMonthlyExpense = averageMonthly(transactions, TransactionType.EXPENSE);
        BigDecimal averageMonthlyNet = averageMonthlyIncome.subtract(averageMonthlyExpense);

        BigDecimal pendingReceivables = pendingByType(obligations, PaymentObligationType.RECEIVABLE);
        BigDecimal pendingPayables = pendingByType(obligations, PaymentObligationType.PAYABLE);
        BigDecimal overduePayables = obligations.stream()
                .filter(o -> o.getType() == PaymentObligationType.PAYABLE)
                .filter(o -> o.getStatus() == PaymentObligationStatus.OVERDUE)
                .map(o -> o.getAmount().subtract(o.getPaidAmount()).max(BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal suggestedSavingsRatio = overduePayables.compareTo(BigDecimal.ZERO) > 0 ? new BigDecimal("0.10") : new BigDecimal("0.15");
        BigDecimal suggestedMonthlySavings = averageMonthlyNet.compareTo(BigDecimal.ZERO) > 0
                ? averageMonthlyNet.multiply(suggestedSavingsRatio).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal emergencyFundTarget = averageMonthlyExpense.multiply(new BigDecimal("3")).setScale(2, RoundingMode.HALF_UP);

        List<TopExpenseCategoryDto> topExpenseCategories = topExpenseCategories(transactions);
        List<String> recommendations = recommendations(currentMonthNet, overduePayables, pendingPayables, suggestedMonthlySavings);

        return InsightsSummaryResponse.builder()
                .currentMonthIncome(currentMonthIncome)
                .currentMonthExpense(currentMonthExpense)
                .currentMonthNet(currentMonthNet)
                .averageMonthlyIncome(averageMonthlyIncome)
                .averageMonthlyExpense(averageMonthlyExpense)
                .pendingReceivables(pendingReceivables)
                .pendingPayables(pendingPayables)
                .overduePayables(overduePayables)
                .suggestedMonthlySavings(suggestedMonthlySavings)
                .emergencyFundTarget(emergencyFundTarget)
                .topExpenseCategories(topExpenseCategories)
                .recommendations(recommendations)
                .disclaimer("Insights are informational and not professional financial advice.")
                .build();
    }

    private BigDecimal sumTransactions(List<Transaction> transactions, TransactionType type, YearMonth month) {
        return transactions.stream()
                .filter(t -> t.getType() == type)
                .filter(t -> YearMonth.from(t.getDate()).equals(month))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal averageMonthly(List<Transaction> transactions, TransactionType type) {
        Map<YearMonth, BigDecimal> byMonth = transactions.stream()
                .filter(t -> t.getType() == type)
                .collect(Collectors.groupingBy(t -> YearMonth.from(t.getDate()),
                        Collectors.mapping(Transaction::getAmount, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));
        if (byMonth.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal total = byMonth.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(byMonth.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal pendingByType(List<PaymentObligation> obligations, PaymentObligationType type) {
        return obligations.stream()
                .filter(o -> o.getType() == type)
                .filter(o -> o.getStatus() != PaymentObligationStatus.PAID && o.getStatus() != PaymentObligationStatus.CANCELLED)
                .map(o -> o.getAmount().subtract(o.getPaidAmount()).max(BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private List<TopExpenseCategoryDto> topExpenseCategories(List<Transaction> transactions) {
        return transactions.stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .collect(Collectors.groupingBy(Transaction::getCategory,
                        Collectors.mapping(Transaction::getAmount, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))))
                .entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue(Comparator.reverseOrder()))
                .limit(3)
                .map(entry -> TopExpenseCategoryDto.builder().category(entry.getKey()).amount(entry.getValue()).build())
                .toList();
    }

    private List<String> recommendations(BigDecimal currentMonthNet, BigDecimal overduePayables, BigDecimal pendingPayables, BigDecimal suggestedMonthlySavings) {
        List<String> recommendations = new ArrayList<>();
        if (overduePayables.compareTo(BigDecimal.ZERO) > 0) {
            recommendations.add("You have overdue payables; prioritize clearing them before discretionary spending.");
        }
        if (currentMonthNet.compareTo(BigDecimal.ZERO) > 0) {
            recommendations.add("Set aside at least " + suggestedMonthlySavings + " this month to build resilient savings.");
        } else {
            recommendations.add("Current month cash flow is negative; trim variable expenses and defer non-essential purchases.");
        }
        if (pendingPayables.compareTo(BigDecimal.ZERO) > 0) {
            recommendations.add("Track upcoming payables weekly to avoid missed due dates and penalties.");
        }
        if (recommendations.isEmpty()) {
            recommendations.add("Keep tracking transactions consistently to improve recommendation quality.");
        }
        return recommendations;
    }
}
