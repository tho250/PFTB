package com.example.pftb.dto;

import com.example.pftb.entity.MessageDetectedType;
import com.example.pftb.entity.MessageEventReviewStatus;
import com.example.pftb.entity.MessageSourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class MessageEventRequest {
    @NotBlank(message = "rawText is required")
    private String rawText;

    @NotNull(message = "sourceType is required")
    private MessageSourceType sourceType;

    private boolean matched;
    private Long matchedRuleId;
    private MessageDetectedType detectedType;
    private BigDecimal amount;
    private String currency;
    private String counterparty;
    private LocalDate eventDate;
    private String category;
    private Double confidenceScore;
    private MessageEventReviewStatus reviewStatus;
    private Long linkedPaymentObligationId;
}
