package com.anastasia.Anastasia_BackEnd.modules.payments.web.controller;

import com.anastasia.Anastasia_BackEnd.modules.payments.application.usecase.HandleSubscriptionWebhookUseCase;
import com.anastasia.Anastasia_BackEnd.modules.payments.application.usecase.HandleWebhookEventUseCase;
import com.anastasia.Anastasia_BackEnd.modules.payments.infrastructure.stripe.StripeWebhookVerifier;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
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

    @PostMapping
    public ResponseEntity<Void> handle(@RequestHeader("Stripe-Signature") String sigHeader,
                                       @RequestBody String payload) {
        try {
            Event event = verifier.verify(payload, sigHeader);
            log.debug("Received Stripe webhook event type={}", event.getType());

            switch (event.getType()) {
                case "checkout.session.completed" -> handleCheckoutCompleted(event);
                case "payment_intent.succeeded" -> handlePaymentSucceeded(event);
                case "customer.subscription.created" -> handleSubscriptionCreated(event);
                case "customer.subscription.deleted" -> handleSubscriptionCanceled(event);
                case "customer.subscription.updated" -> handleSubscriptionUpdated(event);
                default -> log.debug("Ignoring unsupported Stripe event {}", event.getType());
            }
            return ResponseEntity.ok().build();
        } catch (StripeException | IllegalArgumentException e) {
            log.warn("Invalid Stripe webhook: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Stripe webhook handling failed", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private void handleCheckoutCompleted(Event event) {
        event.getDataObjectDeserializer().getObject().ifPresentOrElse(data -> {
            Session session = (Session) data;
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
        }, () -> log.warn("Unable to deserialize checkout.session.completed payload"));
    }

    private void handlePaymentSucceeded(Event event) {
        event.getDataObjectDeserializer().getObject().ifPresentOrElse(data -> {
            PaymentIntent paymentIntent = (PaymentIntent) data;
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
        }, () -> log.warn("Unable to deserialize payment_intent.succeeded payload"));
    }

    private void handleSubscriptionCreated(Event event) {
        event.getDataObjectDeserializer().getObject().ifPresentOrElse(data -> {
            Subscription subscription = (Subscription) data;
            Map<String, String> metadata = subscription.getMetadata();
            if (metadata == null || !metadata.containsKey("subscriptionId")) {
                log.warn("Subscription created event missing subscriptionId metadata: {}", subscription.getId());
                return;
            }
            subscriptionHandler.handleSubscriptionActivated(
                    UUID.fromString(metadata.get("subscriptionId")),
                    subscription.getId());
        }, () -> log.warn("Unable to deserialize customer.subscription.created payload"));
    }

    private void handleSubscriptionCanceled(Event event) {
        event.getDataObjectDeserializer().getObject().ifPresentOrElse(data -> {
            Subscription subscription = (Subscription) data;
            Map<String, String> metadata = subscription.getMetadata();
            if (metadata == null || !metadata.containsKey("subscriptionId")) {
                log.warn("Subscription canceled event missing subscriptionId metadata: {}", subscription.getId());
                return;
            }
            subscriptionHandler.handleSubscriptionCanceled(UUID.fromString(metadata.get("subscriptionId")));
        }, () -> log.warn("Unable to deserialize customer.subscription.deleted payload"));
    }

    private void handleSubscriptionUpdated(Event event) {
        event.getDataObjectDeserializer().getObject().ifPresent(data -> {
            Subscription subscription = (Subscription) data;
            if ("canceled".equalsIgnoreCase(subscription.getStatus())) {
                Map<String, String> metadata = subscription.getMetadata();
                if (metadata != null && metadata.containsKey("subscriptionId")) {
                    subscriptionHandler.handleSubscriptionCanceled(UUID.fromString(metadata.get("subscriptionId")));
                }
            }
        });
    }
}
