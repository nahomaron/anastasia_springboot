package com.anastasia.Anastasia_BackEnd.modules.accounting.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class TransferFundsRequest {
    private UUID tenantId;

    @NotNull(message = "Date is required")
    private LocalDate date;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be positive")
    private BigDecimal amount;

    @NotNull(message = "Source asset account ID is required")
    private Long fromAssetAccountId; // e.g., "Cash on Hand"

    @NotNull(message = "Destination asset account ID is required")
    private Long toAssetAccountId; // e.g., "Main Bank"
}
