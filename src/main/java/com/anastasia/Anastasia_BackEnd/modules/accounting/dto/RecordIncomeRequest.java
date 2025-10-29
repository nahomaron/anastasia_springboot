package com.anastasia.Anastasia_BackEnd.modules.accounting.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class RecordIncomeRequest {
    @NotBlank(message = "Tenant ID is required")
    private String tenantId;

    @NotNull(message = "Date is required")
    private LocalDate date;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be positive")
    private BigDecimal amount;

    @NotNull(message = "Asset account ID is required (e.g., Bank, Cash)")
    private Long assetAccountId; // e.g., "Main Bank"

    @NotNull(message = "Income account ID is required (e.g., Tithes)")
    private Long incomeAccountId; // e.g., "Tithe Income"
}
