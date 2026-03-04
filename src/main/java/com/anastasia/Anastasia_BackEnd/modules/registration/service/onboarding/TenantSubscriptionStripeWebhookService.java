package com.anastasia.Anastasia_BackEnd.modules.registration.service.onboarding;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.SubscriptionStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantSubscriptionEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantSubscriptionRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.SubscriptionService;
import com.stripe.model.Invoice;
import com.stripe.model.Subscription;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantSubscriptionStripeWebhookService {

    private final TenantSubscriptionRepository tenantSubscriptionRepository;
    private final SubscriptionService subscriptionService;

    @Transactional
    public boolean handleInvoicePaid(String eventId, Instant occurredAt, Invoice invoice) {
        String providerSubscriptionId = resolveInvoiceSubscriptionId(invoice);
        if (providerSubscriptionId == null || providerSubscriptionId.isBlank()) {
            return false;
        }

        TenantSubscriptionEntity subscription = tenantSubscriptionRepository
                .findByProviderSubscriptionId(providerSubscriptionId)
                .orElse(null);
        if (subscription == null) {
            return false;
        }

        LocalDateTime occurredAtUtc = toUtc(occurredAt);
        if (isOlderThanLastProcessed(subscription, eventId, occurredAtUtc)) {
            return true;
        }

        subscription.setProviderSubscriptionId(providerSubscriptionId);
        if (invoice.getCustomer() != null && !invoice.getCustomer().isBlank()) {
            subscription.setProviderCustomerId(invoice.getCustomer());
        }
        if (invoice.getPeriodStart() != null) {
            subscription.setCurrentPeriodStartAt(toUtc(Instant.ofEpochSecond(invoice.getPeriodStart())));
        }
        if (invoice.getPeriodEnd() != null) {
            subscription.setCurrentPeriodEndAt(toUtc(Instant.ofEpochSecond(invoice.getPeriodEnd())));
        }

        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setLastPaymentAt(occurredAtUtc);
        subscription.setLastStripeEventId(eventId);
        subscription.setLastStripeEventAt(occurredAtUtc);
        tenantSubscriptionRepository.save(subscription);

        // Renewal payment success is canonical for applying scheduled downgrades.
        subscriptionService.applyDuePendingPlanChange(subscription.getTenant().getId(), null);
        return true;
    }

    @Transactional
    public boolean handleSubscriptionUpdated(String eventId, Instant occurredAt, Subscription stripeSubscription) {
        String providerSubscriptionId = stripeSubscription.getId();
        if (providerSubscriptionId == null || providerSubscriptionId.isBlank()) {
            return false;
        }

        TenantSubscriptionEntity subscription = tenantSubscriptionRepository
                .findByProviderSubscriptionId(providerSubscriptionId)
                .orElse(null);
        if (subscription == null) {
            return false;
        }

        LocalDateTime occurredAtUtc = toUtc(occurredAt);
        if (isOlderThanLastProcessed(subscription, eventId, occurredAtUtc)) {
            return true;
        }

        if (stripeSubscription.getCustomer() != null && !stripeSubscription.getCustomer().isBlank()) {
            subscription.setProviderCustomerId(stripeSubscription.getCustomer());
        }
        subscription.setProviderSubscriptionId(providerSubscriptionId);

        Long currentPeriodStart = resolveCurrentPeriodStart(stripeSubscription);
        Long currentPeriodEnd = resolveCurrentPeriodEnd(stripeSubscription);
        if (currentPeriodStart != null) {
            subscription.setCurrentPeriodStartAt(toUtc(Instant.ofEpochSecond(currentPeriodStart)));
        }
        if (currentPeriodEnd != null) {
            subscription.setCurrentPeriodEndAt(toUtc(Instant.ofEpochSecond(currentPeriodEnd)));
        }

        subscription.setCancelAtPeriodEnd(Boolean.TRUE.equals(stripeSubscription.getCancelAtPeriodEnd()));
        if (stripeSubscription.getCanceledAt() != null) {
            subscription.setCanceledAt(toUtc(Instant.ofEpochSecond(stripeSubscription.getCanceledAt())));
        }

        subscription.setStatus(mapStatus(stripeSubscription.getStatus()));
        subscription.setLastStripeEventId(eventId);
        subscription.setLastStripeEventAt(occurredAtUtc);
        tenantSubscriptionRepository.save(subscription);

        if (subscription.getStatus() == SubscriptionStatus.ACTIVE || subscription.getStatus() == SubscriptionStatus.TRIALING) {
            subscriptionService.applyDuePendingPlanChange(subscription.getTenant().getId(), null);
        }
        return true;
    }

    private boolean isOlderThanLastProcessed(TenantSubscriptionEntity subscription,
                                             String eventId,
                                             LocalDateTime eventTimeUtc) {
        if (eventId != null && eventId.equals(subscription.getLastStripeEventId())) {
            return true;
        }
        if (subscription.getLastStripeEventAt() == null || eventTimeUtc == null) {
            return false;
        }
        return eventTimeUtc.isBefore(subscription.getLastStripeEventAt());
    }

    private LocalDateTime toUtc(Instant instant) {
        if (instant == null) {
            return LocalDateTime.now(ZoneOffset.UTC);
        }
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private SubscriptionStatus mapStatus(String stripeStatus) {
        if (stripeStatus == null) {
            return SubscriptionStatus.SUSPENDED;
        }
        return switch (stripeStatus.toLowerCase()) {
            case "active" -> SubscriptionStatus.ACTIVE;
            case "trialing" -> SubscriptionStatus.TRIALING;
            case "past_due" -> SubscriptionStatus.PAST_DUE;
            case "canceled" -> SubscriptionStatus.CANCELED;
            case "unpaid", "incomplete", "incomplete_expired" -> SubscriptionStatus.SUSPENDED;
            default -> {
                log.debug("Unknown Stripe subscription status '{}', mapping to SUSPENDED", stripeStatus);
                yield SubscriptionStatus.SUSPENDED;
            }
        };
    }

    private String resolveInvoiceSubscriptionId(Invoice invoice) {
        if (invoice == null || invoice.getParent() == null || invoice.getParent().getSubscriptionDetails() == null) {
            return null;
        }
        return invoice.getParent().getSubscriptionDetails().getSubscription();
    }

    private Long resolveCurrentPeriodStart(Subscription stripeSubscription) {
        if (stripeSubscription == null
                || stripeSubscription.getItems() == null
                || stripeSubscription.getItems().getData() == null
                || stripeSubscription.getItems().getData().isEmpty()) {
            return null;
        }
        return stripeSubscription.getItems().getData().stream()
                .map(item -> item != null ? item.getCurrentPeriodStart() : null)
                .filter(java.util.Objects::nonNull)
                .min(Long::compareTo)
                .orElse(null);
    }

    private Long resolveCurrentPeriodEnd(Subscription stripeSubscription) {
        if (stripeSubscription == null
                || stripeSubscription.getItems() == null
                || stripeSubscription.getItems().getData() == null
                || stripeSubscription.getItems().getData().isEmpty()) {
            return null;
        }
        return stripeSubscription.getItems().getData().stream()
                .map(item -> item != null ? item.getCurrentPeriodEnd() : null)
                .filter(java.util.Objects::nonNull)
                .max(Long::compareTo)
                .orElse(null);
    }
}
