package com.example.pftb.service;

import com.example.pftb.dto.PaymentObligationRequest;
import com.example.pftb.dto.PaymentObligationResponse;
import com.example.pftb.dto.PaymentObligationSummaryResponse;
import com.example.pftb.dto.RecordPaymentRequest;
import com.example.pftb.dto.TransactionRequest;
import com.example.pftb.entity.PaymentObligation;
import com.example.pftb.entity.PaymentObligationPriority;
import com.example.pftb.entity.PaymentObligationStatus;
import com.example.pftb.entity.PaymentObligationType;
import com.example.pftb.entity.TransactionType;
import com.example.pftb.entity.User;
import com.example.pftb.repository.PaymentObligationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
public class PaymentObligationService {
    private final PaymentObligationRepository paymentObligationRepository;
    private final UserContextService userContextService;
    private final TransactionService transactionService;

    public PaymentObligationService(PaymentObligationRepository paymentObligationRepository,
                                    UserContextService userContextService,
                                    TransactionService transactionService) {
        this.paymentObligationRepository = paymentObligationRepository;
        this.userContextService = userContextService;
        this.transactionService = transactionService;
    }

    @Transactional(readOnly = true)
    public List<PaymentObligationResponse> list(String username,
                                                PaymentObligationType type,
                                                PaymentObligationStatus status,
                                                PaymentObligationPriority priority,
                                                LocalDate dueBefore,
                                                LocalDate dueAfter) {
        User user = userContextService.getUserByUsername(username);
        return paymentObligationRepository.findByUserIdOrderByDueDateAsc(user.getId())
                .stream()
                .peek(this::refreshStatus)
                .filter(o -> type == null || o.getType() == type)
                .filter(o -> status == null || o.getStatus() == status)
                .filter(o -> priority == null || o.getPriority() == priority)
                .filter(o -> dueBefore == null || !o.getDueDate().isAfter(dueBefore))
                .filter(o -> dueAfter == null || !o.getDueDate().isBefore(dueAfter))
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public PaymentObligationResponse create(String username, PaymentObligationRequest request) {
        User user = userContextService.getUserByUsername(username);
        PaymentObligation obligation = PaymentObligation.builder()
                .user(user)
                .title(request.getTitle())
                .counterparty(request.getCounterparty())
                .type(request.getType())
                .amount(request.getAmount())
                .paidAmount(BigDecimal.ZERO)
                .dueDate(request.getDueDate())
                .priority(request.getPriority())
                .status(PaymentObligationStatus.PENDING)
                .notes(request.getNotes())
                .reminderEnabled(request.isReminderEnabled())
                .build();
        refreshStatus(obligation);
        return mapToResponse(paymentObligationRepository.save(obligation));
    }

    @Transactional
    public PaymentObligationResponse update(String username, Long id, PaymentObligationRequest request) {
        User user = userContextService.getUserByUsername(username);
        PaymentObligation obligation = paymentObligationRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Payment obligation not found"));
        obligation.setTitle(request.getTitle());
        obligation.setCounterparty(request.getCounterparty());
        obligation.setType(request.getType());
        obligation.setAmount(request.getAmount());
        obligation.setDueDate(request.getDueDate());
        obligation.setPriority(request.getPriority());
        obligation.setNotes(request.getNotes());
        obligation.setReminderEnabled(request.isReminderEnabled());
        refreshStatus(obligation);
        return mapToResponse(paymentObligationRepository.save(obligation));
    }

    @Transactional
    public void delete(String username, Long id) {
        User user = userContextService.getUserByUsername(username);
        PaymentObligation obligation = paymentObligationRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Payment obligation not found"));
        paymentObligationRepository.delete(obligation);
    }

    @Transactional
    public PaymentObligationResponse recordPayment(String username, Long id, RecordPaymentRequest request) {
        User user = userContextService.getUserByUsername(username);
        PaymentObligation obligation = paymentObligationRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Payment obligation not found"));
        obligation.setPaidAmount(obligation.getPaidAmount().add(request.getAmount()));
        refreshStatus(obligation);

        if (request.isCreateTransaction()) {
            TransactionRequest txRequest = TransactionRequest.builder()
                    .title("Obligation payment: " + obligation.getTitle())
                    .amount(request.getAmount())
                    .type(obligation.getType() == PaymentObligationType.PAYABLE ? TransactionType.EXPENSE : TransactionType.INCOME)
                    .category("Obligation Payment")
                    .date(request.getDate())
                    .build();
            transactionService.createTransactionForUser(user, txRequest);
        }
        return mapToResponse(paymentObligationRepository.save(obligation));
    }

    @Transactional(readOnly = true)
    public PaymentObligationSummaryResponse summary(String username) {
        User user = userContextService.getUserByUsername(username);
        List<PaymentObligation> obligations = paymentObligationRepository.findByUserIdOrderByDueDateAsc(user.getId());
        obligations.forEach(this::refreshStatus);

        BigDecimal totalReceivablesPending = pendingAmountByType(obligations, PaymentObligationType.RECEIVABLE);
        BigDecimal totalPayablesPending = pendingAmountByType(obligations, PaymentObligationType.PAYABLE);
        long overdueCount = obligations.stream().filter(o -> o.getStatus() == PaymentObligationStatus.OVERDUE).count();
        long dueSoonCount = obligations.stream()
                .filter(o -> o.getStatus() != PaymentObligationStatus.PAID && o.getStatus() != PaymentObligationStatus.CANCELLED)
                .filter(o -> !o.getDueDate().isBefore(LocalDate.now()) && !o.getDueDate().isAfter(LocalDate.now().plusDays(7)))
                .count();
        long highPriorityCount = obligations.stream()
                .filter(o -> o.getPriority() == PaymentObligationPriority.HIGH || o.getPriority() == PaymentObligationPriority.CRITICAL)
                .filter(o -> o.getStatus() != PaymentObligationStatus.PAID && o.getStatus() != PaymentObligationStatus.CANCELLED)
                .count();

        return PaymentObligationSummaryResponse.builder()
                .totalReceivablesPending(totalReceivablesPending)
                .totalPayablesPending(totalPayablesPending)
                .overdueCount(overdueCount)
                .dueSoonCount(dueSoonCount)
                .highPriorityCount(highPriorityCount)
                .build();
    }

    private BigDecimal pendingAmountByType(List<PaymentObligation> obligations, PaymentObligationType type) {
        return obligations.stream()
                .filter(o -> o.getType() == type)
                .filter(o -> o.getStatus() != PaymentObligationStatus.PAID && o.getStatus() != PaymentObligationStatus.CANCELLED)
                .map(o -> o.getAmount().subtract(o.getPaidAmount()))
                .filter(amount -> amount.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void refreshStatus(PaymentObligation obligation) {
        if (Objects.equals(obligation.getStatus(), PaymentObligationStatus.CANCELLED)) {
            return;
        }
        if (obligation.getPaidAmount().compareTo(obligation.getAmount()) >= 0) {
            obligation.setStatus(PaymentObligationStatus.PAID);
            return;
        }
        if (obligation.getPaidAmount().compareTo(BigDecimal.ZERO) > 0) {
            obligation.setStatus(PaymentObligationStatus.PARTIALLY_PAID);
            return;
        }
        if (obligation.getDueDate().isBefore(LocalDate.now())) {
            obligation.setStatus(PaymentObligationStatus.OVERDUE);
            return;
        }
        obligation.setStatus(PaymentObligationStatus.PENDING);
    }

    private PaymentObligationResponse mapToResponse(PaymentObligation obligation) {
        return PaymentObligationResponse.builder()
                .id(obligation.getId())
                .title(obligation.getTitle())
                .counterparty(obligation.getCounterparty())
                .type(obligation.getType())
                .amount(obligation.getAmount())
                .paidAmount(obligation.getPaidAmount())
                .dueDate(obligation.getDueDate())
                .priority(obligation.getPriority())
                .status(obligation.getStatus())
                .notes(obligation.getNotes())
                .reminderEnabled(obligation.isReminderEnabled())
                .createdAt(obligation.getCreatedAt())
                .updatedAt(obligation.getUpdatedAt())
                .build();
    }
}
