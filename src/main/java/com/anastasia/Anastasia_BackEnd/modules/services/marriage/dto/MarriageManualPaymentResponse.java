package com.anastasia.Anastasia_BackEnd.modules.services.marriage.dto;

import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriageManualPaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record MarriageManualPaymentResponse(
        UUID id,
        String paymentCategory,
        BigDecimal amount,
        String currency,
        String receiptReferenceNumber,
        UUID receivedByUserId,
        LocalDate receivedDate,
        MarriageManualPaymentStatus verificationStatus,
        String note
) {
}
