package com.anastasia.Anastasia_BackEnd.modules.payments.application.usecase;

import com.anastasia.Anastasia_BackEnd.modules.payments.domain.model.PaymentIntent;
import com.anastasia.Anastasia_BackEnd.modules.payments.infrastructure.outbox.OutboxPublisher;
import com.anastasia.Anastasia_BackEnd.modules.payments.infrastructure.repository.PaymentIntentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

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
                "PaymentAuthorized",
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

        outbox.publish(
                "PaymentCaptured",
                pi.getId().toString(),
                pi.getTenantId(),
                Map.of(
                        "paymentId", pi.getId().toString(),
                        "providerRef", providerRef,
                        "gross", gross,
                        "fees", fees,
                        "net", net,
                        "status", pi.getStatus().name()
                )
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
