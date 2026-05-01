package com.example.pftb.dto;

import com.example.pftb.entity.MessageSourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MessageParsePreviewRequest {
    @NotBlank(message = "rawText is required")
    private String rawText;

    @NotNull(message = "sourceType is required")
    private MessageSourceType sourceType;
}
