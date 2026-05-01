package com.example.pftb.dto;

import com.example.pftb.entity.PaymentObligationPriority;
import com.example.pftb.entity.PaymentObligationType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PaymentObligationRequest {
    @NotBlank(message = "title is required")
    private String title;

    private String counterparty;

    @NotNull(message = "type is required")
    private PaymentObligationType type;

    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.01", message = "amount must be greater than zero")
    private BigDecimal amount;

    @NotNull(message = "dueDate is required")
    private LocalDate dueDate;

    @NotNull(message = "priority is required")
    private PaymentObligationPriority priority;

    private String notes;

    private boolean reminderEnabled;
}
