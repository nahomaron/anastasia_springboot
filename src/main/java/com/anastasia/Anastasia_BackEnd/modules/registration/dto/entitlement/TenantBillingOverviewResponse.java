package com.anastasia.Anastasia_BackEnd.modules.registration.dto.entitlement;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.BillingInterval;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.BillingOverrideType;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.SubscriptionPlan;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.SubscriptionStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.WorkspaceInitializationMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
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
    private WorkspaceInitializationMode workspaceInitializationMode;
    private boolean demoWorkspace;
    private Instant trialStartAt;
    private Instant trialEndAt;
    private Instant currentPeriodStartAt;
    private Instant currentPeriodEndAt;
    private Instant gracePeriodEndsAt;
    private boolean cancelAtPeriodEnd;
    private SubscriptionPlan pendingPlan;
    private Instant pendingPlanEffectiveAt;
    private Instant scheduledPurgeAt;
    private Instant scheduledDeletionAt;
    private Instant archiveScheduledAt;
    private Instant archivedAt;
    private boolean retentionWarningActive;
    private long normalAmountMinor;
    private long discountAmountMinor;
    private long effectiveAmountMinor;
    private String currency;
    private BillingOverrideType appliedBillingOverrideType;
    private Instant billingOverrideEndsAt;
    private List<SubscriptionPlanHistoryItemResponse> recentPlanHistory;
}
