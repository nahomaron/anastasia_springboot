package com.anastasia.Anastasia_BackEnd.modules.registration.service;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.PlanChangeTiming;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.SubscriptionPlan;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.SubscriptionPlanHistoryEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantSubscriptionEntity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface SubscriptionService {

    /**
     * Starts a trial for a tenant subscription.
     */
    TenantSubscriptionEntity startTrial(UUID tenantId, SubscriptionPlan plan, Instant trialEnd, UUID actorUserId);

    /**
     * Changes subscription plan and records an audit event.
     */
    TenantSubscriptionEntity changePlan(UUID tenantId, SubscriptionPlan newPlan, UUID actorUserId);

    /**
     * Records successful payment and updates subscription state.
     */
    TenantSubscriptionEntity recordPaymentSucceeded(UUID tenantId,
                                                    Instant paymentAt,
                                                    String providerCustomerId,
                                                    String providerSubscriptionId,
                                                    String paymentMethodLast4,
                                                    UUID actorUserId);

    /**
     * Records failed payment attempt and updates subscription state.
     */
    TenantSubscriptionEntity recordPaymentFailed(UUID tenantId, Instant failedAt, UUID actorUserId);

    /**
     * Cancels subscription immediately or at period end.
     */
    TenantSubscriptionEntity cancelSubscription(UUID tenantId, boolean cancelAtPeriodEnd, UUID actorUserId);

    /**
     * Returns tenant subscription by tenant id.
     */
    TenantSubscriptionEntity getByTenantId(UUID tenantId);

    /**
     * Applies lifecycle state transitions such as expiring a free trial into suspension.
     */
    TenantSubscriptionEntity syncSubscriptionState(UUID tenantId, UUID actorUserId);

    /**
     * Request a tenant plan change (immediate for upgrades, period-end for downgrades).
     */
    TenantSubscriptionEntity requestPlanChange(UUID tenantId,
                                               SubscriptionPlan targetPlan,
                                               PlanChangeTiming timing,
                                               String reason,
                                               UUID actorUserId);

    /**
     * Applies pending plan change if effective_at has been reached.
     */
    TenantSubscriptionEntity applyDuePendingPlanChange(UUID tenantId, UUID actorUserId);

    /**
     * Returns recent applied plan history.
     */
    List<SubscriptionPlanHistoryEntity> listRecentPlanHistory(UUID tenantId);
}
