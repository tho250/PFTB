package com.example.pftb.dto;

import com.example.pftb.entity.MessageDetectedType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class MessageParsePreviewResponse {
    private boolean matched;
    private MessageDetectedType detectedType;
    private BigDecimal amount;
    private String currency;
    private String counterparty;
    private String category;
    private Long matchedRuleId;
    private Double confidenceScore;
}
