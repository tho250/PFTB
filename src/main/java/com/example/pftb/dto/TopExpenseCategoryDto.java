package com.example.pftb.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class TopExpenseCategoryDto {
    private String category;
    private BigDecimal amount;
}
