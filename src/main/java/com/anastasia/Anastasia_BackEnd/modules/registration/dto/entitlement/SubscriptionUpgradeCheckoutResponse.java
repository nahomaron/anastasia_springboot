package com.anastasia.Anastasia_BackEnd.modules.registration.dto.entitlement;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.SubscriptionPlan;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantSubscriptionUpgradeStatus;
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
public class SubscriptionUpgradeCheckoutResponse {

    private UUID upgradeRequestId;
    private UUID tenantId;
    private SubscriptionPlan currentPlan;
    private SubscriptionPlan targetPlan;
    private TenantSubscriptionUpgradeStatus status;
    private String currency;
    private Long expectedAmountMinor;
    private String checkoutUrl;
    private String checkoutSessionId;
    private Instant expiresAt;
    private Instant createdAt;
    private Instant updatedAt;
}
