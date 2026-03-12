package com.anastasia.Anastasia_BackEnd.modules.payments.web.controller;

import com.anastasia.Anastasia_BackEnd.modules.payments.application.usecase.HandleSubscriptionWebhookUseCase;
import com.anastasia.Anastasia_BackEnd.modules.payments.application.usecase.HandleWebhookEventUseCase;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.onboarding.OnboardingStripeWebhookService;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.onboarding.TenantSubscriptionStripeWebhookService;
import com.anastasia.Anastasia_BackEnd.modules.payments.stripe.StripeWebhookVerifier;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.Invoice;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/webhooks/stripe")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final StripeWebhookVerifier verifier;
    private final HandleWebhookEventUseCase paymentHandler;
    private final HandleSubscriptionWebhookUseCase subscriptionHandler;
    private final OnboardingStripeWebhookService onboardingStripeWebhookService;
    private final TenantSubscriptionStripeWebhookService tenantSubscriptionStripeWebhookService;

    @PostMapping
    public ResponseEntity<Void> handle(@RequestHeader("Stripe-Signature") String sigHeader,
                                       @RequestBody String payload) {
        try {
            log.info("Stripe webhook received: payloadSize={}, signaturePresent={}",
                    payload != null ? payload.length() : 0,
                    sigHeader != null && !sigHeader.isBlank());
            Event event = verifier.verify(payload, sigHeader);
            log.info("Stripe webhook verified: id={}, type={}", event.getId(), event.getType());

            switch (event.getType()) {
                case "checkout.session.completed" -> handleCheckoutCompleted(event, payload, sigHeader);
                case "payment_intent.succeeded" -> handlePaymentSucceeded(event);
                case "customer.subscription.created" -> handleSubscriptionCreated(event, payload, sigHeader);
                case "customer.subscription.deleted" -> handleSubscriptionCanceled(event, payload, sigHeader);
                case "customer.subscription.updated" -> handleSubscriptionUpdated(event, payload, sigHeader);
                case "invoice.paid" -> handleInvoicePaid(event, payload, sigHeader);
                default -> log.debug("Ignoring unsupported Stripe event {}", event.getType());
            }
            log.info("Stripe webhook processed: id={}, type={}", event.getId(), event.getType());
            return ResponseEntity.ok().build();
        } catch (StripeException | IllegalArgumentException e) {
            log.warn("Invalid Stripe webhook: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Stripe webhook handling failed", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // checkout was successful (but not captured yet)
    private void handleCheckoutCompleted(Event event, String payload, String sigHeader) {
        Session session = deserializeStripeObject(event, Session.class, "checkout.session.completed");
        if (session == null) {
            return;
        }
        {
            Instant occurredAt = event.getCreated() != null ? Instant.ofEpochSecond(event.getCreated()) : Instant.now();
            if (onboardingStripeWebhookService.handleCheckoutSessionCompleted(
                    event.getId(), event.getType(), occurredAt, payload, sigHeader, session)) {
                return;
            }
            Map<String, String> metadata = session.getMetadata();
            if (metadata == null) {
                log.warn("Checkout session missing metadata: {}", session.getId());
                return;
            }

            if ("subscription".equalsIgnoreCase(session.getMode())) {
                String subscriptionId = metadata.get("subscriptionId");
                if (subscriptionId == null) {
                    log.warn("Subscription checkout missing subscriptionId metadata: {}", session.getId());
                    return;
                }
                if (session.getSubscription() != null) {
                    subscriptionHandler.handleSubscriptionActivated(
                            UUID.fromString(subscriptionId),
                            session.getSubscription());
                } else {
                    log.debug("Subscription checkout completed without subscription id yet; awaiting subscription.created event");
                }
                return;
            }

            String paymentId = metadata.get("paymentId");
            if (paymentId == null) {
                log.warn("Checkout session missing paymentId metadata: {}", session.getId());
                return;
            }
            if (session.getPaymentIntent() == null) {
                log.warn("Checkout session missing payment intent id: {}", session.getId());
                return;
            }

            Long amountMinor = session.getAmountTotal();
            paymentHandler.handleAuthorized(
                    UUID.fromString(paymentId),
                    session.getPaymentIntent(),
                    event.getId(),
                    event.getType(),
                    event.getCreated() != null ? Instant.ofEpochSecond(event.getCreated()) : Instant.now(),
                    amountMinor
            );
        }
    }

    // means the payment has been captured (money transferred)
    private void handlePaymentSucceeded(Event event) {
        PaymentIntent paymentIntent = deserializeStripeObject(event, PaymentIntent.class, "payment_intent.succeeded");
        if (paymentIntent == null) {
            return;
        }
        {
            Map<String, String> metadata = paymentIntent.getMetadata();
            if (metadata == null || !metadata.containsKey("paymentId")) {
                log.warn("PaymentIntent succeeded event missing paymentId metadata: {}", paymentIntent.getId());
                return;
            }

            String paymentId = metadata.get("paymentId");
            long gross = paymentIntent.getAmountReceived() != null
                    ? paymentIntent.getAmountReceived()
                    : paymentIntent.getAmount();

            // Fees require Stripe Balance Transactions API; keep placeholder for now
            paymentHandler.handleCaptured(
                    UUID.fromString(paymentId),
                    paymentIntent.getId(),
                    gross,
                    0L,
                    gross,
                    paymentIntent.getCurrency(),
                    event.getId(),
                    event.getType(),
                    event.getCreated() != null ? Instant.ofEpochSecond(event.getCreated()) : Instant.now()
            );
        }
    }

    private void handleSubscriptionCreated(Event event, String payload, String sigHeader) {
        Subscription subscription = deserializeStripeObject(event, Subscription.class, "customer.subscription.created");
        if (subscription == null) {
            return;
        }
        {
            Instant occurredAt = event.getCreated() != null ? Instant.ofEpochSecond(event.getCreated()) : Instant.now();
            if (onboardingStripeWebhookService.handleSubscriptionEvent(
                    event.getId(), event.getType(), occurredAt, payload, sigHeader, subscription)) {
                return;
            }
            Map<String, String> metadata = subscription.getMetadata();
            if (metadata == null || !metadata.containsKey("subscriptionId")) {
                log.warn("Subscription created event missing subscriptionId metadata: {}", subscription.getId());
                return;
            }
            subscriptionHandler.handleSubscriptionActivated(
                    UUID.fromString(metadata.get("subscriptionId")),
                    subscription.getId());
        }
    }

    private void handleSubscriptionCanceled(Event event, String payload, String sigHeader) {
        Subscription subscription = deserializeStripeObject(event, Subscription.class, "customer.subscription.deleted");
        if (subscription == null) {
            return;
        }
        {
            Instant occurredAt = event.getCreated() != null ? Instant.ofEpochSecond(event.getCreated()) : Instant.now();
            if (onboardingStripeWebhookService.handleSubscriptionEvent(
                    event.getId(), event.getType(), occurredAt, payload, sigHeader, subscription)) {
                return;
            }
            if (tenantSubscriptionStripeWebhookService.handleSubscriptionUpdated(event.getId(), occurredAt, payload, sigHeader, subscription)) {
                return;
            }
            Map<String, String> metadata = subscription.getMetadata();
            if (metadata == null || !metadata.containsKey("subscriptionId")) {
                log.warn("Subscription canceled event missing subscriptionId metadata: {}", subscription.getId());
                return;
            }
            subscriptionHandler.handleSubscriptionCanceled(UUID.fromString(metadata.get("subscriptionId")));
        }
    }

    private void handleSubscriptionUpdated(Event event, String payload, String sigHeader) {
        Subscription subscription = deserializeStripeObject(event, Subscription.class, "customer.subscription.updated");
        if (subscription == null) {
            return;
        }
        {
            Instant occurredAt = event.getCreated() != null ? Instant.ofEpochSecond(event.getCreated()) : Instant.now();
            if (onboardingStripeWebhookService.handleSubscriptionEvent(
                    event.getId(), event.getType(), occurredAt, payload, sigHeader, subscription)) {
                return;
            }
            if (tenantSubscriptionStripeWebhookService.handleSubscriptionUpdated(event.getId(), occurredAt, payload, sigHeader, subscription)) {
                return;
            }
            if ("canceled".equalsIgnoreCase(subscription.getStatus())) {
                Map<String, String> metadata = subscription.getMetadata();
                if (metadata != null && metadata.containsKey("subscriptionId")) {
                    subscriptionHandler.handleSubscriptionCanceled(UUID.fromString(metadata.get("subscriptionId")));
                }
            }
        }
    }

    private void handleInvoicePaid(Event event, String payload, String sigHeader) {
        Invoice invoice = deserializeStripeObject(event, Invoice.class, "invoice.paid");
        if (invoice == null) {
            return;
        }
        {
            Instant occurredAt = event.getCreated() != null ? Instant.ofEpochSecond(event.getCreated()) : Instant.now();
            if (onboardingStripeWebhookService.handleInvoicePaid(event.getId(), event.getType(), occurredAt, payload, sigHeader, invoice)) {
                return;
            }
            tenantSubscriptionStripeWebhookService.handleInvoicePaid(event.getId(), occurredAt, payload, sigHeader, invoice);
        }
    }

    private <T extends StripeObject> T deserializeStripeObject(Event event, Class<T> type, String eventType) {
        return event.getDataObjectDeserializer().getObject()
                .filter(type::isInstance)
                .map(type::cast)
                .orElseGet(() -> {
                    try {
                        StripeObject unsafe = event.getDataObjectDeserializer().deserializeUnsafe();
                        if (type.isInstance(unsafe)) {
                            log.warn("Unsafe-deserialized Stripe event payload for type={}", eventType);
                            return type.cast(unsafe);
                        }
                    } catch (Exception ex) {
                        log.warn("Unsafe deserialization failed for Stripe event type={} : {}", eventType, ex.getMessage());
                    }
                    log.warn("Unable to deserialize Stripe payload for event type={}", eventType);
                    return null;
                });
    }
}
