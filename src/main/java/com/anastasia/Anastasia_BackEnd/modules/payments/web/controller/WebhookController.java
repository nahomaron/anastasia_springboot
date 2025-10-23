package com.anastasia.Anastasia_BackEnd.modules.payments.web.controller;

import com.anastasia.Anastasia_BackEnd.modules.payments.application.usecase.HandleWebhookEventUseCase;
import com.anastasia.Anastasia_BackEnd.modules.payments.infrastructure.stripe.StripeWebhookVerifier;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/webhooks/stripe")
@RequiredArgsConstructor
public class WebhookController {

    private final StripeWebhookVerifier verifier;
    private final HandleWebhookEventUseCase handler;

    @PostMapping
    public ResponseEntity<Void> handle(@RequestHeader("Stripe-Signature") String sigHeader,
                                       @RequestBody String payload,
                                       @RequestHeader("X-Tenant-Id") String tenantId) {
        try {
            Event event = verifier.verify(payload, sigHeader);

            switch (event.getType()) {
                case "checkout.session.completed" -> {
                    Session session = (Session) event.getDataObjectDeserializer()
                            .getObject().orElse(null);
                    if (session != null) {
                        String paymentId = session.getMetadata().get("paymentId");
                        // Authorized == payment confirmed on checkout completion. Capture may be immediate depending on mode.
                        handler.handleAuthorized(UUID.fromString(paymentId), session.getId(), tenantId);
                    }
                }
                case "payment_intent.succeeded" -> {
                    var pi = (com.stripe.model.PaymentIntent) event.getDataObjectDeserializer()
                            .getObject().orElse(null);
                    if (pi != null) {
                        String paymentId = pi.getMetadata().get("paymentId");
                        long gross = pi.getAmountReceived() != null ? pi.getAmountReceived() : pi.getAmount();
                        // Fees require Stripe Balance Transactions API; here we send zero placeholder
                        handler.handleCaptured(UUID.fromString(paymentId), pi.getId(), tenantId, gross, 0L, gross);
                    }
                }
                default -> { /* ignore for now */ }
            }
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
