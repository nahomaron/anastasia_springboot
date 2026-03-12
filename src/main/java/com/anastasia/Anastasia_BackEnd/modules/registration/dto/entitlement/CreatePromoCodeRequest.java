package com.anastasia.Anastasia_BackEnd.modules.registration.dto.entitlement;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.SubscriptionPlan;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantFeature;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePromoCodeRequest {
    @NotBlank
    private String code;
    @NotBlank
    private String name;
    private String description;
    private SubscriptionPlan grantedPlan;
    private Set<TenantFeature> grantedFeatures;
    private Integer activeMemberLimitOverride;
    private Integer maxRedemptions;
    private boolean oneTimePerTenant;
    private Instant expiresAt;
}
