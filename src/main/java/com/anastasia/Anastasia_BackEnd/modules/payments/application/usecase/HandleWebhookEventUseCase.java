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
    public void handleAuthorized(UUID paymentId, String providerRef) {
        var pi = paymentRepo.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown payment intent: " + paymentId));

        enforceStripeProvider(pi, providerRef);
        pi.markAuthorized(providerRef);
        paymentRepo.save(pi);

        outbox.publish(
                PaymentEventType.PAYMENT_AUTHORIZED,
                pi.getId().toString(),
                pi.getTenantId(),
                Map.of(
                        "paymentId", pi.getId().toString(),
                        "providerRef", providerRef,
                        "status", pi.getStatus().name()
                )
        );
    }

    @Transactional
    public void handleCaptured(UUID paymentId, String providerRef, long gross, long fees, long net) {
        var pi = paymentRepo.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown payment intent: " + paymentId));

        enforceStripeProvider(pi, providerRef);
        pi.markCaptured(providerRef);
        paymentRepo.save(pi);

        Map<String, Object> payload = new HashMap<>();
        payload.put("paymentId", pi.getId().toString());
        payload.put("providerRef", providerRef);
        payload.put("gross", gross);
        payload.put("fees", fees);
        payload.put("net", net);
        payload.put("currency", pi.getAmount() != null ? pi.getAmount().getCurrency() : null);
        payload.put("status", pi.getStatus().name());
        payload.put("tenantId", pi.getTenantId() != null ? pi.getTenantId().toString() : null);
        payload.put("purpose", pi.getPurpose() != null ? pi.getPurpose().name() : null);
        payload.put("fundId", pi.getFundId());
        payload.put("memberId", pi.getMemberId());
        payload.put("capturedAt", Instant.now().toString());

        outbox.publish(
                PaymentEventType.PAYMENT_CAPTURED,
                pi.getId().toString(),
                pi.getTenantId(),
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
