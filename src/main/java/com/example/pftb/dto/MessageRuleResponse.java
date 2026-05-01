package com.example.pftb.dto;

import com.example.pftb.entity.MessageDirectionType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class MessageRuleResponse {
    private Long id;
    private String name;
    private boolean enabled;
    private MessageDirectionType directionType;
    private List<String> keywords;
    private String amountRegex;
    private String counterpartyRegex;
    private String category;
    private String priority;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
