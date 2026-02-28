package com.anastasia.Anastasia_BackEnd.modules.registration.service;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.SubscriptionPlan;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantSubscriptionEntity;

import java.time.LocalDateTime;
import java.util.UUID;

public interface SubscriptionService {

    /**
     * Starts a trial for a tenant subscription.
     */
    TenantSubscriptionEntity startTrial(UUID tenantId, SubscriptionPlan plan, LocalDateTime trialEnd, UUID actorUserId);

    /**
     * Changes subscription plan and records an audit event.
     */
    TenantSubscriptionEntity changePlan(UUID tenantId, SubscriptionPlan newPlan, UUID actorUserId);

    /**
     * Records successful payment and updates subscription state.
     */
    TenantSubscriptionEntity recordPaymentSucceeded(UUID tenantId,
                                                    LocalDateTime paymentAt,
                                                    String providerCustomerId,
                                                    String providerSubscriptionId,
                                                    String paymentMethodLast4,
                                                    UUID actorUserId);

    /**
     * Records failed payment attempt and updates subscription state.
     */
    TenantSubscriptionEntity recordPaymentFailed(UUID tenantId, LocalDateTime failedAt, UUID actorUserId);

    /**
     * Cancels subscription immediately or at period end.
     */
    TenantSubscriptionEntity cancelSubscription(UUID tenantId, boolean cancelAtPeriodEnd, UUID actorUserId);
}
