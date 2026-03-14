package com.anastasia.Anastasia_BackEnd.modules.registration.dto.entitlement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromoRedemptionResponse {
    private UUID id;
    private UUID tenantId;
    private UUID promoCodeId;
    private String promoCode;
    private boolean active;
    private Instant redeemedAt;
    private Instant expiresAt;
    private Instant revokedAt;
    private String reason;
    private UUID createdByUserId;
    private UUID updatedByUserId;
    private Instant createdAt;
    private Instant updatedAt;
}
