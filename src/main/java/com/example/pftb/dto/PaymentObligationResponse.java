package com.example.pftb.dto;

import com.example.pftb.entity.PaymentObligationPriority;
import com.example.pftb.entity.PaymentObligationStatus;
import com.example.pftb.entity.PaymentObligationType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class PaymentObligationResponse {
    private Long id;
    private String title;
    private String counterparty;
    private PaymentObligationType type;
    private BigDecimal amount;
    private BigDecimal paidAmount;
    private LocalDate dueDate;
    private PaymentObligationPriority priority;
    private PaymentObligationStatus status;
    private String notes;
    private boolean reminderEnabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
