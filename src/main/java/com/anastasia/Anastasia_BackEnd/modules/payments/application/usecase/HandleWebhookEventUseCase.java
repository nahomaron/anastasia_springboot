package com.anastasia.Anastasia_BackEnd.modules.payments.application.usecase;

import com.anastasia.Anastasia_BackEnd.modules.payments.domain.model.PaymentIntent;
import com.anastasia.Anastasia_BackEnd.modules.payments.domain.model.PaymentStatus;
import com.anastasia.Anastasia_BackEnd.modules.payments.infrastructure.outbox.OutboxPublisher;
import com.anastasia.Anastasia_BackEnd.modules.payments.infrastructure.repository.PaymentIntentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HandleWebhookEventUseCase {

    private final PaymentIntentRepository paymentRepo;
    private final OutboxPublisher outbox;

    @Transactional
    public void handleAuthorized(UUID paymentId, String providerRef, String tenantId) {
        var pi = paymentRepo.findById(paymentId).orElseThrow();
        pi.markAuthorized(providerRef);
        paymentRepo.save(pi);

        outbox.publish(
                "PaymentAuthorized",
                pi.getId().toString(),
                tenantId,
                Map.of("paymentId", pi.getId().toString(), "providerRef", providerRef, "status", "AUTHORIZED")
        );
    }

    @Transactional
    public void handleCaptured(UUID paymentId, String providerRef, String tenantId, long gross, long fees, long net) {
        var pi = paymentRepo.findById(paymentId).orElseThrow();
        pi.markCaptured();
        paymentRepo.save(pi);

        outbox.publish(
                "PaymentCaptured",
                pi.getId().toString(),
                tenantId,
                Map.of("paymentId", pi.getId().toString(),
                        "providerRef", providerRef,
                        "gross", gross, "fees", fees, "net", net)
        );
    }
}
