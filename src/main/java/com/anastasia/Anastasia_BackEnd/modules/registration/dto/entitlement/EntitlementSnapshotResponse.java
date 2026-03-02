package com.anastasia.Anastasia_BackEnd.modules.registration.dto.entitlement;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.SubscriptionPlan;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantFeature;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntitlementSnapshotResponse {
    private UUID tenantId;
    private SubscriptionPlan basePlan;
    private SubscriptionPlan effectivePlan;
    private Set<TenantFeature> features;
    private Map<String, Integer> limits;
}
