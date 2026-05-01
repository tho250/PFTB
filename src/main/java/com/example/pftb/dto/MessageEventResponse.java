package com.example.pftb.dto;

import com.example.pftb.entity.MessageDetectedType;
import com.example.pftb.entity.MessageEventReviewStatus;
import com.example.pftb.entity.MessageSourceType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class MessageEventResponse {
    private Long id;
    private String rawText;
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
    private Long linkedTransactionId;
    private Long linkedPaymentObligationId;
    private LocalDateTime createdAt;
}
