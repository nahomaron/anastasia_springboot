package com.anastasia.Anastasia_BackEnd.modules.registration.service.onboarding;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.BillingProvider;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.SubscriptionStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantSubscriptionProviderLinkEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.WebhookEventReceiptEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.WebhookProcessingResult;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.WebhookEventReceiptRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantSubscriptionProviderLinkRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.SubscriptionService;
import com.stripe.model.Invoice;
import com.stripe.model.Subscription;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantSubscriptionStripeWebhookService {

    private static final String PROVIDER = "STRIPE";

    private final TenantSubscriptionProviderLinkRepository tenantSubscriptionProviderLinkRepository;
    private final WebhookEventReceiptRepository webhookEventReceiptRepository;
    private final SubscriptionService subscriptionService;

    @Transactional
    public boolean handleInvoicePaid(String eventId,
                                     Instant occurredAt,
                                     String payload,
                                     String signatureHeader,
                                     Invoice invoice) {
        String providerSubscriptionId = resolveInvoiceSubscriptionId(invoice);
        if (providerSubscriptionId == null || providerSubscriptionId.isBlank()) {
            return false;
        }

        TenantSubscriptionProviderLinkEntity providerLink = tenantSubscriptionProviderLinkRepository
                .findByProviderAndProviderSubscriptionId(BillingProvider.STRIPE, providerSubscriptionId)
                .orElse(null);
        if (providerLink == null) {
            return false;
        }
        return processEvent(eventId, "invoice.paid", occurredAt, payload, signatureHeader, providerLink, () -> {
            var subscription = providerLink.getTenantSubscription();
            Instant occurredAtUtc = occurredAt != null ? occurredAt : Instant.now();
            if (isOlderThanLastProcessed(providerLink, eventId, occurredAtUtc)) {
                return true;
            }

            providerLink.setProviderSubscriptionId(providerSubscriptionId);
            if (invoice.getCustomer() != null && !invoice.getCustomer().isBlank()) {
                providerLink.setProviderCustomerId(invoice.getCustomer());
            }
            if (invoice.getPeriodStart() != null) {
                subscription.setCurrentPeriodStartAt(Instant.ofEpochSecond(invoice.getPeriodStart()));
            }
            if (invoice.getPeriodEnd() != null) {
                subscription.setCurrentPeriodEndAt(Instant.ofEpochSecond(invoice.getPeriodEnd()));
            }

            subscription.setStatus(SubscriptionStatus.ACTIVE);
            subscription.setLastPaymentAt(occurredAtUtc);
            providerLink.setProviderStatus("active");
            providerLink.setLastProviderEventId(eventId);
            providerLink.setLastProviderEventType("invoice.paid");
            providerLink.setLastProviderEventAt(occurredAtUtc);
            tenantSubscriptionProviderLinkRepository.save(providerLink);

            subscriptionService.applyDuePendingPlanChange(subscription.getTenant().getId(), null);
            return true;
        });
    }

    @Transactional
    public boolean handleSubscriptionUpdated(String eventId,
                                             Instant occurredAt,
                                             String payload,
                                             String signatureHeader,
                                             Subscription stripeSubscription) {
        String providerSubscriptionId = stripeSubscription.getId();
        if (providerSubscriptionId == null || providerSubscriptionId.isBlank()) {
            return false;
        }

        TenantSubscriptionProviderLinkEntity providerLink = tenantSubscriptionProviderLinkRepository
                .findByProviderAndProviderSubscriptionId(BillingProvider.STRIPE, providerSubscriptionId)
                .orElse(null);
        if (providerLink == null) {
            return false;
        }
        return processEvent(eventId, "customer.subscription.updated", occurredAt, payload, signatureHeader, providerLink, () -> {
            var subscription = providerLink.getTenantSubscription();
            Instant occurredAtUtc = occurredAt != null ? occurredAt : Instant.now();
            if (isOlderThanLastProcessed(providerLink, eventId, occurredAtUtc)) {
                return true;
            }

            if (stripeSubscription.getCustomer() != null && !stripeSubscription.getCustomer().isBlank()) {
                providerLink.setProviderCustomerId(stripeSubscription.getCustomer());
            }
            providerLink.setProviderSubscriptionId(providerSubscriptionId);

            Long currentPeriodStart = resolveCurrentPeriodStart(stripeSubscription);
            Long currentPeriodEnd = resolveCurrentPeriodEnd(stripeSubscription);
            if (currentPeriodStart != null) {
                subscription.setCurrentPeriodStartAt(Instant.ofEpochSecond(currentPeriodStart));
            }
            if (currentPeriodEnd != null) {
                subscription.setCurrentPeriodEndAt(Instant.ofEpochSecond(currentPeriodEnd));
            }

            subscription.setCancelAtPeriodEnd(Boolean.TRUE.equals(stripeSubscription.getCancelAtPeriodEnd()));
            if (stripeSubscription.getCanceledAt() != null) {
                subscription.setCanceledAt(Instant.ofEpochSecond(stripeSubscription.getCanceledAt()));
            }

            subscription.setStatus(mapStatus(stripeSubscription.getStatus()));
            providerLink.setProviderStatus(stripeSubscription.getStatus());
            providerLink.setLastProviderEventId(eventId);
            providerLink.setLastProviderEventType("customer.subscription.updated");
            providerLink.setLastProviderEventAt(occurredAtUtc);
            tenantSubscriptionProviderLinkRepository.save(providerLink);

            if (subscription.getStatus() == SubscriptionStatus.ACTIVE || subscription.getStatus() == SubscriptionStatus.TRIALING) {
                subscriptionService.applyDuePendingPlanChange(subscription.getTenant().getId(), null);
            }
            return true;
        });
    }

    private boolean processEvent(String eventId,
                                 String eventType,
                                 Instant occurredAt,
                                 String payload,
                                 String signatureHeader,
                                 TenantSubscriptionProviderLinkEntity providerLink,
                                 Supplier<Boolean> handler) {
        Optional<WebhookEventReceiptEntity> existing = webhookEventReceiptRepository
                .findByProviderAndEventId(PROVIDER, eventId);

        if (existing.isPresent() && existing.get().getProcessingResult() == WebhookProcessingResult.OK) {
            return true;
        }

        var subscription = providerLink.getTenantSubscription();
        WebhookEventReceiptEntity receipt = existing.orElseGet(() -> WebhookEventReceiptEntity.builder()
                .provider(PROVIDER)
                .eventId(eventId)
                .build());
        receipt.setEventType(eventType);
        receipt.setTenantId(subscription.getTenant().getId());
        receipt.setTenantSubscriptionId(subscription.getId());
        receipt.setEventCreatedAt(occurredAt != null ? occurredAt : Instant.now());
        if (receipt.getPayload() == null) {
            receipt.setPayload(payload);
        }
        if (receipt.getSignatureHeader() == null) {
            receipt.setSignatureHeader(signatureHeader);
        }
        receipt.setProcessingResult(WebhookProcessingResult.RETRY);
        receipt.setErrorMessage(null);
        receipt.setProcessedAt(null);
        webhookEventReceiptRepository.save(receipt);

        try {
            boolean handled = handler.get();
            receipt.setProcessingResult(WebhookProcessingResult.OK);
            receipt.setProcessedAt(Instant.now());
            webhookEventReceiptRepository.save(receipt);
            return handled;
        } catch (RuntimeException ex) {
            receipt.setProcessingResult(WebhookProcessingResult.FAILED);
            receipt.setProcessedAt(Instant.now());
            receipt.setErrorMessage(trimError(ex.getMessage()));
            webhookEventReceiptRepository.save(receipt);
            log.error("Failed to process Stripe tenant subscription webhook event {} ({})", eventId, eventType, ex);
            return false;
        }
    }

    private boolean isOlderThanLastProcessed(TenantSubscriptionProviderLinkEntity providerLink,
                                             String eventId,
                                             Instant eventTimeUtc) {
        if (eventId != null && eventId.equals(providerLink.getLastProviderEventId())) {
            return true;
        }
        if (providerLink.getLastProviderEventAt() == null || eventTimeUtc == null) {
            return false;
        }
        return eventTimeUtc.isBefore(providerLink.getLastProviderEventAt());
    }

    private SubscriptionStatus mapStatus(String stripeStatus) {
        if (stripeStatus == null) {
            return SubscriptionStatus.SUSPENDED;
        }
        return switch (stripeStatus.toLowerCase(Locale.ROOT)) {
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

    private String trimError(String message) {
        if (message == null || message.isBlank()) {
            return "Unknown webhook processing error";
        }
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }
}
