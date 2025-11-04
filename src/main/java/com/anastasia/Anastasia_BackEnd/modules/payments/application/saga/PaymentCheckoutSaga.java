package com.anastasia.Anastasia_BackEnd.modules.payments.application.saga;

import com.anastasia.Anastasia_BackEnd.modules.payments.application.usecase.CreatePaymentIntentUseCase;
import com.anastasia.Anastasia_BackEnd.modules.payments.domain.events.PaymentEventType;
import com.anastasia.Anastasia_BackEnd.modules.payments.domain.model.PaymentIntent;
import com.anastasia.Anastasia_BackEnd.modules.payments.domain.model.PaymentPurpose;
import com.anastasia.Anastasia_BackEnd.core.outbox.OutboxPublisher;
import com.anastasia.Anastasia_BackEnd.modules.payments.repository.PaymentIntentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Coordinates the happy-path checkout flow by delegating to the intent use case and
 * emitting the domain events required by downstream modules (notifications, accounting, etc).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentCheckoutSaga {

    private final PaymentIntentRepository intentRepository;
    private final CreatePaymentIntentUseCase createIntentUseCase;
    private final OutboxPublisher outboxPublisher;

    /**
     * Starts (or resumes) a checkout flow for the given tenant/idempotency pair.
     * If an intent already exists we simply return it; otherwise we create a new one
     * and enqueue a {@link PaymentEventType#PAYMENT_INITIATED} outbox event.
     */
    @Transactional
    public PaymentIntent startCheckout(UUID tenantId,
                                       PaymentPurpose purpose,
                                       long amountMinor,
                                       String currency,
                                       Long memberId,
                                       UUID userId,
                                       String userEmail,
                                       String fundId,
                                       String idempotencyKey) {

        return intentRepository.findByTenantIdAndIdempotencyKey(tenantId, idempotencyKey)
                .map(existing -> {
                    log.debug("Reusing existing payment intent {} for tenant={} idempotencyKey={}",
                            existing.getId(), tenantId, idempotencyKey);
                    return existing;
                })
                .orElseGet(() -> createAndPublish(tenantId, purpose, amountMinor, currency, memberId, userId, userEmail, fundId, idempotencyKey));
    }

    private PaymentIntent createAndPublish(UUID tenantId,
                                           PaymentPurpose purpose,
                                           long amountMinor,
                                           String currency,
                                           Long memberId,
                                           UUID userId,
                                           String userEmail,
                                           String fundId,
                                           String idempotencyKey) {
        PaymentIntent intent = createIntentUseCase.execute(
                tenantId,
                purpose,
                amountMinor,
                currency,
                memberId,
                userId,
                userEmail,
                fundId,
                idempotencyKey);

        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("paymentId", intent.getId().toString());
        payload.put("tenantId", intent.getTenantId() != null ? intent.getTenantId().toString() : null);
        payload.put("status", intent.getStatus().name());
        payload.put("purpose", intent.getPurpose().name());
        payload.put("amountMinor", intent.getAmount() != null ? intent.getAmount().getAmount() : amountMinor);
        payload.put("currency", intent.getAmount() != null ? intent.getAmount().getCurrency() : currency);
        payload.put("fundId", intent.getFundId());
        payload.put("memberId", intent.getMemberId());
        payload.put("userId", intent.getUserId() != null ? intent.getUserId().toString() : null);
        payload.put("userEmail", intent.getUserEmail());


        outboxPublisher.publish(
                PaymentEventType.PAYMENT_INITIATED,
                tenantId,
                intent.getId().toString(),
                intent
        );

        return intent;
    }
}
