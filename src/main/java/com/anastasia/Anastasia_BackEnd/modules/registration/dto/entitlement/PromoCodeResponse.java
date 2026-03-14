package com.anastasia.Anastasia_BackEnd.modules.registration.dto.entitlement;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.SubscriptionPlan;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantFeature;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromoCodeResponse {
    private UUID id;
    private String code;
    private String name;
    private String description;
    private SubscriptionPlan grantedPlan;
    private Set<TenantFeature> grantedFeatures;
    private Integer activeMemberLimitOverride;
    private boolean active;
    private Integer maxRedemptions;
    private int currentRedemptions;
    private boolean oneTimePerTenant;
    private Instant expiresAt;
    private Instant activatedAt;
    private Instant deactivatedAt;
    private UUID createdByUserId;
    private UUID updatedByUserId;
    private Instant createdAt;
    private Instant updatedAt;
}
