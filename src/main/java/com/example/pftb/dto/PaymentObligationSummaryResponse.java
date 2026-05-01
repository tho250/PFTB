package com.example.pftb.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PaymentObligationSummaryResponse {
    private BigDecimal totalReceivablesPending;
    private BigDecimal totalPayablesPending;
    private long overdueCount;
    private long dueSoonCount;
    private long highPriorityCount;
}
