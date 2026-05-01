package com.example.pftb.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class InsightsSummaryResponse {
    private BigDecimal currentMonthIncome;
    private BigDecimal currentMonthExpense;
    private BigDecimal currentMonthNet;
    private BigDecimal averageMonthlyIncome;
    private BigDecimal averageMonthlyExpense;
    private BigDecimal pendingReceivables;
    private BigDecimal pendingPayables;
    private BigDecimal overduePayables;
    private BigDecimal suggestedMonthlySavings;
    private BigDecimal emergencyFundTarget;
    private List<TopExpenseCategoryDto> topExpenseCategories;
    private List<String> recommendations;
    private String disclaimer;
}
