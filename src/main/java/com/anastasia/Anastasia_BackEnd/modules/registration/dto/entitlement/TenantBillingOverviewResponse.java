package com.anastasia.Anastasia_BackEnd.modules.registration.dto.entitlement;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.BillingInterval;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.SubscriptionPlan;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.SubscriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantBillingOverviewResponse {
    private UUID tenantId;
    private SubscriptionPlan currentPlan;
    private SubscriptionStatus status;
    private BillingInterval billingInterval;
    private LocalDateTime currentPeriodStartAt;
    private LocalDateTime currentPeriodEndAt;
    private boolean cancelAtPeriodEnd;
    private SubscriptionPlan pendingPlan;
    private LocalDateTime pendingPlanEffectiveAt;
    private List<SubscriptionPlanHistoryItemResponse> recentPlanHistory;
}
