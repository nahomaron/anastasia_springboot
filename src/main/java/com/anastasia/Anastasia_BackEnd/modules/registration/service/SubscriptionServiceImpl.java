package com.anastasia.Anastasia_BackEnd.modules.registration.service;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.BillingProvider;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.PlanChangeTiming;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.SubscriptionPlan;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.SubscriptionPlanHistoryEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.SubscriptionStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantSubscriptionEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantSubscriptionEventEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantSubscriptionEventType;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.SubscriptionPlanHistoryRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantSubscriptionEventRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantSubscriptionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {

    private final TenantSubscriptionRepository tenantSubscriptionRepository;
    private final TenantSubscriptionEventRepository tenantSubscriptionEventRepository;
    private final SubscriptionPlanHistoryRepository subscriptionPlanHistoryRepository;
    private final TenantRepository tenantRepository;

    @Override
    @Transactional
    public TenantSubscriptionEntity startTrial(UUID tenantId, SubscriptionPlan plan, LocalDateTime trialEnd, UUID actorUserId) {
        if (trialEnd != null && trialEnd.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("trialEnd cannot be in the past");
        }

        TenantSubscriptionEntity subscription = requireOrCreate(tenantId, actorUserId);

        SubscriptionPlan oldPlan = subscription.getPlan();
        SubscriptionStatus oldStatus = subscription.getStatus();

        subscription.setPlan(plan != null ? plan : SubscriptionPlan.FREE);
        subscription.setStatus(SubscriptionStatus.TRIALING);
        subscription.setTrialStartAt(LocalDateTime.now());
        subscription.setTrialEndAt(trialEnd);
        subscription.setCurrentPeriodStartAt(LocalDateTime.now());
        subscription.setCurrentPeriodEndAt(trialEnd);
        subscription.setCancelAtPeriodEnd(false);
        subscription.setCanceledAt(null);
        subscription.setUpdatedByUserId(actorUserId);

        TenantSubscriptionEntity saved = tenantSubscriptionRepository.save(subscription);
        recordEvent(saved, TenantSubscriptionEventType.CREATED, oldPlan, saved.getPlan(), oldStatus, saved.getStatus(), actorUserId, null);
        return saved;
    }

    @Override
    @Transactional
    public TenantSubscriptionEntity changePlan(UUID tenantId, SubscriptionPlan newPlan, UUID actorUserId) {
        if (newPlan == null) {
            throw new IllegalArgumentException("newPlan is required");
        }

        TenantSubscriptionEntity subscription = requireByTenantId(tenantId);
        SubscriptionPlan oldPlan = subscription.getPlan();

        subscription.setPlan(newPlan);
        subscription.setUpdatedByUserId(actorUserId);

        TenantSubscriptionEntity saved = tenantSubscriptionRepository.save(subscription);
        recordEvent(saved, TenantSubscriptionEventType.PLAN_CHANGED, oldPlan, saved.getPlan(), saved.getStatus(), saved.getStatus(), actorUserId, null);
        return saved;
    }

    @Override
    @Transactional
    public TenantSubscriptionEntity recordPaymentSucceeded(UUID tenantId,
                                                           LocalDateTime paymentAt,
                                                           String providerCustomerId,
                                                           String providerSubscriptionId,
                                                           String paymentMethodLast4,
                                                           UUID actorUserId) {
        TenantSubscriptionEntity subscription = requireByTenantId(tenantId);
        SubscriptionStatus oldStatus = subscription.getStatus();

        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setLastPaymentAt(paymentAt != null ? paymentAt : LocalDateTime.now());
        subscription.setProviderCustomerId(providerCustomerId);
        subscription.setProviderSubscriptionId(providerSubscriptionId);
        subscription.setPaymentMethodLast4(normalizeLast4(paymentMethodLast4));
        if (providerSubscriptionId != null || providerCustomerId != null) {
            subscription.setProvider(BillingProvider.STRIPE);
        }
        if (subscription.getCurrentPeriodStartAt() == null) {
            subscription.setCurrentPeriodStartAt(LocalDateTime.now());
        }
        subscription.setCancelAtPeriodEnd(false);
        subscription.setCanceledAt(null);
        subscription.setUpdatedByUserId(actorUserId);

        TenantSubscriptionEntity saved = tenantSubscriptionRepository.save(subscription);
        recordEvent(saved, TenantSubscriptionEventType.PAYMENT_SUCCEEDED, saved.getPlan(), saved.getPlan(), oldStatus, saved.getStatus(), actorUserId, null);
        return saved;
    }

    @Override
    @Transactional
    public TenantSubscriptionEntity recordPaymentFailed(UUID tenantId, LocalDateTime failedAt, UUID actorUserId) {
        TenantSubscriptionEntity subscription = requireByTenantId(tenantId);
        SubscriptionStatus oldStatus = subscription.getStatus();

        subscription.setStatus(SubscriptionStatus.PAST_DUE);
        subscription.setUpdatedByUserId(actorUserId);

        TenantSubscriptionEntity saved = tenantSubscriptionRepository.save(subscription);
        String idempotencyKey = "payment-failed-" + tenantId + "-" + (failedAt != null ? failedAt : LocalDateTime.now());
        recordEvent(saved, TenantSubscriptionEventType.PAYMENT_FAILED, saved.getPlan(), saved.getPlan(), oldStatus, saved.getStatus(), actorUserId, idempotencyKey);
        return saved;
    }

    @Override
    @Transactional
    public TenantSubscriptionEntity cancelSubscription(UUID tenantId, boolean cancelAtPeriodEnd, UUID actorUserId) {
        TenantSubscriptionEntity subscription = requireByTenantId(tenantId);
        SubscriptionStatus oldStatus = subscription.getStatus();

        subscription.setCancelAtPeriodEnd(cancelAtPeriodEnd);
        if (!cancelAtPeriodEnd) {
            subscription.setStatus(SubscriptionStatus.CANCELED);
            subscription.setCanceledAt(LocalDateTime.now());
        }
        subscription.setUpdatedByUserId(actorUserId);

        TenantSubscriptionEntity saved = tenantSubscriptionRepository.save(subscription);
        recordEvent(saved, TenantSubscriptionEventType.CANCELED, saved.getPlan(), saved.getPlan(), oldStatus, saved.getStatus(), actorUserId, null);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public TenantSubscriptionEntity getByTenantId(UUID tenantId) {
        return requireByTenantId(tenantId);
    }

    @Override
    @Transactional
    public TenantSubscriptionEntity requestPlanChange(UUID tenantId,
                                                      SubscriptionPlan targetPlan,
                                                      PlanChangeTiming timing,
                                                      String reason,
                                                      UUID actorUserId) {
        if (targetPlan == null) {
            throw new IllegalArgumentException("targetPlan is required");
        }

        TenantSubscriptionEntity subscription = requireByTenantId(tenantId);
        SubscriptionPlan currentPlan = subscription.getPlan() != null ? subscription.getPlan() : SubscriptionPlan.FREE;

        if (currentPlan == targetPlan && subscription.getPendingPlan() == null) {
            throw new IllegalArgumentException("Target plan is already active");
        }

        boolean isUpgrade = targetPlan.rank() > currentPlan.rank();
        PlanChangeTiming resolvedTiming = timing != null
                ? timing
                : (isUpgrade ? PlanChangeTiming.IMMEDIATE : PlanChangeTiming.PERIOD_END);

        if (isUpgrade && resolvedTiming == PlanChangeTiming.PERIOD_END) {
            throw new IllegalArgumentException("Upgrades must be immediate");
        }
        if (!isUpgrade && resolvedTiming == PlanChangeTiming.IMMEDIATE) {
            throw new IllegalArgumentException("Downgrades can only be scheduled at period end");
        }

        if (resolvedTiming == PlanChangeTiming.IMMEDIATE) {
            SubscriptionPlan oldPlan = subscription.getPlan();
            subscription.setPlan(targetPlan);
            subscription.setPendingPlan(null);
            subscription.setPendingPlanEffectiveAt(null);
            subscription.setUpdatedByUserId(actorUserId);

            TenantSubscriptionEntity saved = tenantSubscriptionRepository.save(subscription);
            recordEvent(saved, TenantSubscriptionEventType.PLAN_CHANGED, oldPlan, targetPlan,
                    saved.getStatus(), saved.getStatus(), actorUserId, null);
            recordPlanHistory(saved, oldPlan, targetPlan, LocalDateTime.now(), reason, actorUserId, null);
            return saved;
        }

        LocalDateTime scheduledAt = subscription.getCurrentPeriodEndAt() != null
                ? subscription.getCurrentPeriodEndAt()
                : LocalDateTime.now().plusMonths(1);
        subscription.setPendingPlan(targetPlan);
        subscription.setPendingPlanEffectiveAt(scheduledAt);
        subscription.setUpdatedByUserId(actorUserId);

        TenantSubscriptionEntity saved = tenantSubscriptionRepository.save(subscription);
        recordEvent(saved, TenantSubscriptionEventType.PLAN_CHANGE_SCHEDULED, currentPlan, targetPlan,
                saved.getStatus(), saved.getStatus(), actorUserId, null);
        return saved;
    }

    @Override
    @Transactional
    public TenantSubscriptionEntity applyDuePendingPlanChange(UUID tenantId, UUID actorUserId) {
        TenantSubscriptionEntity subscription = requireByTenantId(tenantId);
        if (subscription.getPendingPlan() == null || subscription.getPendingPlanEffectiveAt() == null) {
            return subscription;
        }
        if (subscription.getPendingPlanEffectiveAt().isAfter(LocalDateTime.now())) {
            return subscription;
        }

        SubscriptionPlan oldPlan = subscription.getPlan();
        SubscriptionPlan newPlan = subscription.getPendingPlan();
        subscription.setPlan(newPlan);
        subscription.setPendingPlan(null);
        subscription.setPendingPlanEffectiveAt(null);
        subscription.setUpdatedByUserId(actorUserId);

        TenantSubscriptionEntity saved = tenantSubscriptionRepository.save(subscription);
        recordEvent(saved, TenantSubscriptionEventType.PLAN_CHANGED, oldPlan, newPlan,
                saved.getStatus(), saved.getStatus(), actorUserId, null);
        recordPlanHistory(saved, oldPlan, newPlan, LocalDateTime.now(), "Scheduled plan change applied", actorUserId, null);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionPlanHistoryEntity> listRecentPlanHistory(UUID tenantId) {
        return subscriptionPlanHistoryRepository.findTop20ByTenantIdOrderByEffectiveAtDesc(tenantId);
    }

    private TenantSubscriptionEntity requireByTenantId(UUID tenantId) {
        return tenantSubscriptionRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Tenant subscription not found"));
    }

    private TenantSubscriptionEntity requireOrCreate(UUID tenantId, UUID actorUserId) {
        return tenantSubscriptionRepository.findByTenantId(tenantId)
                .orElseGet(() -> {
                    TenantEntity tenant = tenantRepository.findById(tenantId)
                            .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));

                    TenantSubscriptionEntity created = TenantSubscriptionEntity.builder()
                            .tenant(tenant)
                            .plan(SubscriptionPlan.FREE)
                            .status(SubscriptionStatus.TRIALING)
                            .provider(BillingProvider.MANUAL)
                            .createdByUserId(actorUserId)
                            .updatedByUserId(actorUserId)
                            .build();
                    return tenantSubscriptionRepository.save(created);
                });
    }

    private void recordEvent(TenantSubscriptionEntity subscription,
                             TenantSubscriptionEventType eventType,
                             SubscriptionPlan oldPlan,
                             SubscriptionPlan newPlan,
                             SubscriptionStatus oldStatus,
                             SubscriptionStatus newStatus,
                             UUID actorUserId,
                             String idempotencyKey) {
        TenantSubscriptionEventEntity event = TenantSubscriptionEventEntity.builder()
                .tenantSubscription(subscription)
                .tenant(subscription.getTenant())
                .eventType(eventType)
                .oldPlan(oldPlan)
                .newPlan(newPlan)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .actorUserId(actorUserId)
                .idempotencyKey(idempotencyKey)
                .build();
        tenantSubscriptionEventRepository.save(event);
    }

    private void recordPlanHistory(TenantSubscriptionEntity subscription,
                                   SubscriptionPlan oldPlan,
                                   SubscriptionPlan newPlan,
                                   LocalDateTime effectiveAt,
                                   String reason,
                                   UUID actorUserId,
                                   String stripeEventId) {
        subscriptionPlanHistoryRepository.save(SubscriptionPlanHistoryEntity.builder()
                .tenantId(subscription.getTenant().getId())
                .tenantSubscriptionId(subscription.getId())
                .oldPlan(oldPlan)
                .newPlan(newPlan)
                .effectiveAt(effectiveAt != null ? effectiveAt : LocalDateTime.now())
                .reason(reason)
                .actorUserId(actorUserId)
                .stripeEventId(stripeEventId)
                .build());
    }

    private String normalizeLast4(String paymentMethodLast4) {
        if (paymentMethodLast4 == null || paymentMethodLast4.isBlank()) {
            return null;
        }
        String normalized = paymentMethodLast4.trim();
        if (normalized.length() <= 4) {
            return normalized;
        }
        return normalized.substring(normalized.length() - 4);
    }
}
