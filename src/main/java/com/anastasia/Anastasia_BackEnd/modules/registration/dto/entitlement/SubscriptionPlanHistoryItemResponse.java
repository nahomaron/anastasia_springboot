package com.anastasia.Anastasia_BackEnd.modules.registration.dto.entitlement;

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
public class SubscriptionPlanHistoryItemResponse {
    private UUID id;
    private SubscriptionPlan oldPlan;
    private SubscriptionPlan newPlan;
    private Instant effectiveAt;
    private String reason;
    private UUID actorUserId;
}
