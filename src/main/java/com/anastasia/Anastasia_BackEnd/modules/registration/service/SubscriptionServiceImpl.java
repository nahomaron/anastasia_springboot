package com.anastasia.Anastasia_BackEnd.modules.registration.service;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.BillingProvider;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.SubscriptionPlan;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.SubscriptionStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantSubscriptionEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantSubscriptionEventEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantSubscriptionEventType;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantSubscriptionEventRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantSubscriptionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {

    private final TenantSubscriptionRepository tenantSubscriptionRepository;
    private final TenantSubscriptionEventRepository tenantSubscriptionEventRepository;
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
