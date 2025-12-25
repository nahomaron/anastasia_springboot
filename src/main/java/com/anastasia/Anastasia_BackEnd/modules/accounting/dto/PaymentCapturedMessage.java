package com.anastasia.Anastasia_BackEnd.modules.accounting.dto;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PaymentCapturedMessage {
    UUID tenantId;
    String paymentId;
    String providerRef;
    String purpose;
    String currency;
    Long grossAmountMinor;
    Long netAmountMinor;
    Long feeAmountMinor;
    String fundId;
    String memberId;
    Instant capturedAt;
}
