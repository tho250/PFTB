package com.example.pftb.dto;

import com.example.pftb.entity.MessageDirectionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class MessageRuleRequest {
    @NotBlank(message = "name is required")
    private String name;

    private boolean enabled = true;

    @NotNull(message = "directionType is required")
    private MessageDirectionType directionType;

    @NotEmpty(message = "at least one keyword is required")
    private List<String> keywords;

    private String amountRegex;
    private String counterpartyRegex;
    private String category;
    private String priority;
}
