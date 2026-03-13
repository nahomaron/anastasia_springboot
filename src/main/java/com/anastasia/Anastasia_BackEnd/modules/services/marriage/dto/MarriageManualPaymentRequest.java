package com.anastasia.Anastasia_BackEnd.modules.services.marriage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MarriageManualPaymentRequest(
        @NotBlank String paymentCategory,
        @NotNull BigDecimal amount,
        @NotBlank String currency,
        String receiptReferenceNumber,
        LocalDate receivedDate,
        String note
) {
}
