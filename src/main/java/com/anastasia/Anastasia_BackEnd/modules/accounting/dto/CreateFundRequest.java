package com.anastasia.Anastasia_BackEnd.modules.accounting.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateFundRequest {
    @NotBlank(message = "Tenant ID is required")
    private String tenantId;

    @NotBlank(message = "Fund name is required")
    private String name;

    private String description;
    private BigDecimal goalAmount;
}
