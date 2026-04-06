package com.anastasia.Anastasia_BackEnd.modules.platform.admin.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class PlatformPaymentRecordResponse {
    private UUID paymentId;
    private String tenantName;
    private long amount;
    private String currency;
    private String status;
    private String method;
    private Instant capturedAt;
    private String invoiceNumber;
    private String note;
}
