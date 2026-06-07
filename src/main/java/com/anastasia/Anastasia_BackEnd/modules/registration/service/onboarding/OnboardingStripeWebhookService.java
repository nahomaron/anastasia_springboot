package com.anastasia.Anastasia_BackEnd.modules.registration.service.onboarding;

import com.anastasia.Anastasia_BackEnd.common.i18n.LocalizedMessageService;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.OnboardingSessionStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantOnboardingSessionEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.WebhookEventReceiptEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.WebhookProcessingResult;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantOnboardingSessionRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.WebhookEventReceiptRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.Invoice;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Slf4j
public class OnboardingStripeWebhookService {

    private static final String PROVIDER = "STRIPE";

    private final TenantOnboardingSessionRepository onboardingSessionRepository;
    private final WebhookEventReceiptRepository webhookEventReceiptRepository;
    private final TenantPlanBillingCatalog billingCatalog;
    private final TenantOnboardingProvisioningService onboardingProvisioningService;
    private final LocalizedMessageService messageService;

    @Transactional
    public boolean handleCheckoutSessionCompleted(String eventId,
                                                  String eventType,
                                                  Instant eventCreatedAt,
                                                  String payload,
                                                  String signatureHeader,
                                                  Session session) {
        UUID onboardingSessionId = extractOnboardingSessionId(session.getMetadata());
        if (onboardingSessionId == null) {
            return false;
        }

        return processEvent(eventId, eventType, eventCreatedAt, payload, signatureHeader, onboardingSessionId, () -> {
            TenantOnboardingSessionEntity onboardingSession = requireSession(onboardingSessionId);
            if (isTerminal(onboardingSession.getStatus())) {
                return true;
            }

            onboardingSession.setProviderCheckoutSessionId(session.getId());
            if (session.getSubscription() != null && !session.getSubscription().isBlank()) {
                onboardingSession.setProviderSubscriptionId(session.getSubscription());
            }
            if (session.getCustomer() != null && !session.getCustomer().isBlank()) {
                onboardingSession.setProviderCustomerId(session.getCustomer());
            }

            onboardingSession.setStatus(OnboardingSessionStatus.PAYMENT_PENDING);
            if (onboardingSession.getCheckoutCreatedAt() == null) {
                onboardingSession.setCheckoutCreatedAt(eventCreatedAt != null ? eventCreatedAt : Instant.now());
            }
            onboardingSessionRepository.save(onboardingSession);
            return true;
        });
    }

    @Transactional
    public boolean handleSubscriptionEvent(String eventId,
                                           String eventType,
                                           Instant eventCreatedAt,
                                           String payload,
                                           String signatureHeader,
                                           Subscription subscription) {
        UUID onboardingSessionId = extractOnboardingSessionId(subscription.getMetadata());
        if (onboardingSessionId == null) {
            return false;
        }

        return processEvent(eventId, eventType, eventCreatedAt, payload, signatureHeader, onboardingSessionId, () -> {
            TenantOnboardingSessionEntity onboardingSession = requireSession(onboardingSessionId);
            if (isTerminal(onboardingSession.getStatus())) {
                return true;
            }

            verifyPriceMapping(onboardingSession, subscription);
            applySubscriptionRefs(onboardingSession, subscription);

            String stripeStatus = subscription.getStatus() == null ? "" : subscription.getStatus().toLowerCase(Locale.ROOT);
            if ("active".equals(stripeStatus) || "trialing".equals(stripeStatus)) {
                onboardingSession.setStatus(OnboardingSessionStatus.PAYMENT_CONFIRMED);
                if (onboardingSession.getPaymentConfirmedAt() == null) {
                    onboardingSession.setPaymentConfirmedAt(eventCreatedAt != null ? eventCreatedAt : Instant.now());
                }
            } else if ("canceled".equals(stripeStatus) || "unpaid".equals(stripeStatus)) {
                onboardingSession.setStatus(OnboardingSessionStatus.CANCELED);
                onboardingSession.setFailureReason(messageService.get(
                        "onboarding.stripe.subscription.status",
                        "Stripe subscription status is {0}",
                        stripeStatus
                ));
            } else {
                onboardingSession.setStatus(OnboardingSessionStatus.PAYMENT_PENDING);
            }

            onboardingSessionRepository.save(onboardingSession);
            if (onboardingSession.getStatus() == OnboardingSessionStatus.PAYMENT_CONFIRMED) {
                onboardingProvisioningService.finalizeProvisioningIfReady(onboardingSession.getId());
            }
            return true;
        });
    }

    @Transactional
    public boolean handleInvoicePaid(String eventId,
                                     String eventType,
                                     Instant eventCreatedAt,
                                     String payload,
                                     String signatureHeader,
                                     Invoice invoice) {
        String subscriptionId = resolveInvoiceSubscriptionId(invoice);
        if (subscriptionId == null || subscriptionId.isBlank()) {
            return false;
        }

        TenantOnboardingSessionEntity knownSession = onboardingSessionRepository
                .findByProviderSubscriptionId(subscriptionId)
                .orElse(null);

        Subscription subscription;
        try {
            subscription = Subscription.retrieve(subscriptionId);
        } catch (StripeException e) {
            throw new IllegalStateException(messageService.get(
                    "onboarding.stripe.invoice.subscriptionFetchFailed",
                    "Failed to retrieve Stripe subscription for invoice.paid"
            ), e);
        }

        UUID onboardingSessionId = knownSession != null
                ? knownSession.getId()
                : extractOnboardingSessionId(subscription.getMetadata());
        if (onboardingSessionId == null) {
            return false;
        }

        return processEvent(eventId, eventType, eventCreatedAt, payload, signatureHeader, onboardingSessionId, () -> {
            TenantOnboardingSessionEntity onboardingSession = requireSession(onboardingSessionId);
            if (isTerminal(onboardingSession.getStatus())) {
                return true;
            }

            verifyPriceMapping(onboardingSession, subscription);
            applySubscriptionRefs(onboardingSession, subscription);
            onboardingSession.setStatus(OnboardingSessionStatus.PAYMENT_CONFIRMED);
            if (onboardingSession.getPaymentConfirmedAt() == null) {
                onboardingSession.setPaymentConfirmedAt(eventCreatedAt != null ? eventCreatedAt : Instant.now());
            }
            onboardingSessionRepository.save(onboardingSession);
            onboardingProvisioningService.finalizeProvisioningIfReady(onboardingSession.getId());
            return true;
        });
    }

    private boolean processEvent(String eventId,
                                 String eventType,
                                 Instant eventCreatedAt,
                                 String payload,
                                 String signatureHeader,
                                 UUID onboardingSessionId,
                                 Supplier<Boolean> handler) {
        Optional<WebhookEventReceiptEntity> existing = webhookEventReceiptRepository
                .findByProviderAndEventId(PROVIDER, eventId);

        if (existing.isPresent() && existing.get().getProcessingResult() == WebhookProcessingResult.OK) {
            return true;
        }

        WebhookEventReceiptEntity receipt = existing.orElseGet(() -> WebhookEventReceiptEntity.builder()
                .provider(PROVIDER)
                .eventId(eventId)
                .build());
        receipt.setEventType(eventType);
        receipt.setOnboardingSessionId(onboardingSessionId);
        receipt.setEventCreatedAt(java.util.Objects.requireNonNullElse(eventCreatedAt, Instant.now()));
        receipt.setPayload(WebhookReceiptSanitizer.summarizePayload(payload));
        receipt.setSignatureHeader(null);
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
            log.error("Failed to process Stripe onboarding webhook event {} ({})", eventId, eventType, ex);
            return false;
        }
    }

    private TenantOnboardingSessionEntity requireSession(UUID onboardingSessionId) {
        return onboardingSessionRepository.findById(onboardingSessionId)
                .orElseThrow(() -> new IllegalArgumentException(messageService.get(
                        "onboarding.session.notFound.withId",
                        "Onboarding session not found: {0}",
                        onboardingSessionId
                )));
    }

    private void verifyPriceMapping(TenantOnboardingSessionEntity onboardingSession, Subscription subscription) {
        String expectedPriceId = billingCatalog.resolve(onboardingSession.getSelectedPlan()).getPriceId();
        boolean matchingPrice = subscription.getItems() != null
                && subscription.getItems().getData() != null
                && subscription.getItems().getData().stream()
                .anyMatch(item -> item.getPrice() != null && expectedPriceId.equals(item.getPrice().getId()));
        if (!matchingPrice) {
            String found = subscription.getItems() == null || subscription.getItems().getData() == null
                    ? "none"
                    : subscription.getItems().getData().stream()
                    .map(item -> item.getPrice() != null ? item.getPrice().getId() : null)
                    .filter(java.util.Objects::nonNull)
                    .reduce((a, b) -> a + "," + b)
                    .orElse("none");
            throw new IllegalStateException(messageService.get(
                    "onboarding.stripe.priceMismatch",
                    "Stripe price mismatch for onboarding session {0}. expected={1} found={2}",
                    onboardingSession.getId(), expectedPriceId, found
            ));
        }
    }

    private void applySubscriptionRefs(TenantOnboardingSessionEntity onboardingSession, Subscription subscription) {
        if (subscription.getId() != null && !subscription.getId().isBlank()) {
            onboardingSession.setProviderSubscriptionId(subscription.getId());
        }
        if (subscription.getCustomer() != null && !subscription.getCustomer().isBlank()) {
            onboardingSession.setProviderCustomerId(subscription.getCustomer());
        }
    }

    private UUID extractOnboardingSessionId(Map<String, String> metadata) {
        if (metadata == null) {
            return null;
        }
        String value = metadata.get("onboardingSessionId");
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            log.warn("Invalid onboardingSessionId metadata: {}", value);
            return null;
        }
    }

    private boolean isTerminal(OnboardingSessionStatus status) {
        return status == OnboardingSessionStatus.PROVISIONED
                || status == OnboardingSessionStatus.CANCELED
                || status == OnboardingSessionStatus.EXPIRED;
    }

    private String trimError(String message) {
        if (message == null || message.isBlank()) {
            return messageService.get(
                    "onboarding.webhook.unknownError",
                    "Unknown webhook processing error"
            );
        }
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }

    private String resolveInvoiceSubscriptionId(Invoice invoice) {
        if (invoice == null || invoice.getParent() == null || invoice.getParent().getSubscriptionDetails() == null) {
            return null;
        }
        return invoice.getParent().getSubscriptionDetails().getSubscription();
    }
}
