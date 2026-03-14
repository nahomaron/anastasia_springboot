package com.anastasia.Anastasia_BackEnd.modules.registration.dto.entitlement;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.GrantSource;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.SubscriptionPlan;
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
public class TenantPlanGrantResponse {
    private UUID id;
    private UUID tenantId;
    private SubscriptionPlan grantedPlan;
    private GrantSource source;
    private String promoCode;
    private Integer activeMemberLimitOverride;
    private boolean active;
    private Instant startsAt;
    private Instant expiresAt;
    private Instant revokedAt;
    private String reason;
    private UUID createdByUserId;
    private UUID updatedByUserId;
    private Instant createdAt;
    private Instant updatedAt;
}
