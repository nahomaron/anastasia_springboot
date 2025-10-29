package com.anastasia.Anastasia_BackEnd.modules.accounting.dto;

import java.time.Instant;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PaymentCapturedMessage {
    String tenantId;
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
