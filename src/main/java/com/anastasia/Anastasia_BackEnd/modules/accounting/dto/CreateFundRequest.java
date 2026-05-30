package com.anastasia.Anastasia_BackEnd.modules.accounting.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class CreateFundRequest {
    private UUID tenantId;

    @NotBlank(message = "Fund name is required")
    private String name;

    private String description;
    private BigDecimal goalAmount;
}
