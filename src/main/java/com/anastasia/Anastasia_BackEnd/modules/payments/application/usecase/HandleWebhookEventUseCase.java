package com.anastasia.Anastasia_BackEnd.modules.payments.application.usecase;

import com.anastasia.Anastasia_BackEnd.modules.payments.domain.events.PaymentEventType;
import com.anastasia.Anastasia_BackEnd.modules.payments.domain.model.PaymentIntent;
import com.anastasia.Anastasia_BackEnd.modules.payments.infrastructure.outbox.OutboxPublisher;
import com.anastasia.Anastasia_BackEnd.modules.payments.infrastructure.repository.PaymentIntentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Use case for handling webhook events from payment providers.
 * Processes authorized and captured events for payment intents.
 * Validates the payment intent and provider reference.
 * Updates the payment intent status and saves it to the repository.
 * Publishes relevant events to the outbox for further processing.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HandleWebhookEventUseCase {

    private final PaymentIntentRepository paymentRepo;
    private final OutboxPublisher outbox;

    @Transactional
    public void handleAuthorized(UUID paymentId,
                                 String providerRef,
                                 String stripeEventId,
                                 String stripeEventType,
                                 Instant occurredAt,
                                 Long amountMinor) {
        var pi = paymentRepo.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown payment intent: " + paymentId));

        enforceStripeProvider(pi, providerRef);
        pi.recordStripeEvent(stripeEventId, stripeEventType, occurredAt);
        pi.markAuthorized(providerRef, occurredAt, amountMinor);
        paymentRepo.save(pi);

        Map<String, Object> payload = new HashMap<>();
        payload.put("paymentId", pi.getId().toString());
        payload.put("providerRef", providerRef);
        payload.put("status", pi.getStatus().name());
        payload.put("authorizedAt", pi.getAuthorizedAt() != null ? pi.getAuthorizedAt().toString() : null);
        payload.put("authorizedAmountMinor", pi.getAuthorizedAmountMinor());
        payload.put("stripeEventId", pi.getLastStripeEventId());
        payload.put("stripeEventType", pi.getLastStripeEventType());
        payload.put("tenantId", pi.getTenantId() != null ? pi.getTenantId().toString() : null);
        payload.put("memberId", pi.getMemberId());
        payload.put("userId", pi.getUserId() != null ? pi.getUserId().toString() : null);
        payload.put("userEmail", pi.getUserEmail());

        outbox.publish(
                PaymentEventType.PAYMENT_AUTHORIZED,
                pi.getId().toString(),
                pi.getTenantId(),
                pi.getUserEmail(),
                payload
        );
    }

    @Transactional
    public void handleCaptured(UUID paymentId,
                               String providerRef,
                               long gross,
                               long fees,
                               long net,
                               String currency,
                               String stripeEventId,
                               String stripeEventType,
                               Instant occurredAt) {
        var pi = paymentRepo.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown payment intent: " + paymentId));

        enforceStripeProvider(pi, providerRef);
        pi.recordStripeEvent(stripeEventId, stripeEventType, occurredAt);
        pi.markCaptured(providerRef, gross, fees, net, currency, occurredAt);
        paymentRepo.save(pi);

        Map<String, Object> payload = new HashMap<>();
        payload.put("paymentId", pi.getId().toString());
        payload.put("providerRef", providerRef);
        payload.put("gross", gross);
        payload.put("fees", fees);
        payload.put("net", net);
        payload.put("currency", currency != null ? currency : (pi.getAmount() != null ? pi.getAmount().getCurrency() : null));
        payload.put("status", pi.getStatus().name());
        payload.put("tenantId", pi.getTenantId() != null ? pi.getTenantId().toString() : null);
        payload.put("purpose", pi.getPurpose() != null ? pi.getPurpose().name() : null);
        payload.put("fundId", pi.getFundId());
        payload.put("memberId", pi.getMemberId());
        payload.put("capturedAt", pi.getCapturedAt() != null ? pi.getCapturedAt().toString() : Instant.now().toString());
        payload.put("stripeEventId", pi.getLastStripeEventId());
        payload.put("stripeEventType", pi.getLastStripeEventType());
        payload.put("userId", pi.getUserId() != null ? pi.getUserId().toString() : null);
        payload.put("userEmail", pi.getUserEmail());

        outbox.publish(
                PaymentEventType.PAYMENT_CAPTURED,
                pi.getId().toString(),
                pi.getTenantId(),
                pi.getUserEmail(),
                payload
        );
    }

    private void enforceStripeProvider(PaymentIntent pi, String providerRef) {
        if (!"STRIPE".equalsIgnoreCase(pi.getProvider())) {
            throw new IllegalStateException("Unsupported payment provider for webhook intent " + pi.getId());
        }
        if (providerRef == null || providerRef.isBlank()) {
            throw new IllegalArgumentException("providerRef missing for payment " + pi.getId());
        }
        String currentRef = pi.getProviderRef();
        if (currentRef != null && !currentRef.equals(providerRef)) {
            log.error("Provider reference mismatch for payment {} (expected={}, received={})",
                    pi.getId(), currentRef, providerRef);
            throw new IllegalStateException("Provider reference mismatch for payment " + pi.getId());
        }
    }
}
