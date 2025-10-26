package com.anastasia.Anastasia_BackEnd.modules.payments.application.usecase;

import com.anastasia.Anastasia_BackEnd.modules.payments.domain.events.PaymentEventType;
import com.anastasia.Anastasia_BackEnd.modules.payments.domain.model.PaymentPurpose;
import com.anastasia.Anastasia_BackEnd.modules.payments.domain.model.PaymentSubscription;
import com.anastasia.Anastasia_BackEnd.modules.payments.infrastructure.outbox.OutboxPublisher;
import com.anastasia.Anastasia_BackEnd.modules.payments.infrastructure.repository.PaymentSubscriptionRepository;
import com.anastasia.Anastasia_BackEnd.modules.payments.infrastructure.stripe.StripeClient;
import com.stripe.exception.StripeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreateSubscriptionUseCase {

    private final PaymentSubscriptionRepository subscriptionRepository;
    private final StripeClient stripeClient;
    private final OutboxPublisher outboxPublisher;

    @Transactional
    public PaymentSubscription execute(String tenantId,
                                       PaymentPurpose purpose,
                                       long amountMinor,
                                       String currency,
                                       String memberId,
                                       String fundId,
                                       String idempotencyKey) {

        String normalizedTenantId = Objects.requireNonNull(tenantId, "tenantId must not be null").trim();
        String normalizedIdempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null").trim();
        String normalizedCurrency = Objects.requireNonNull(currency, "currency must not be null").trim().toUpperCase(Locale.ROOT);

        var existing = subscriptionRepository.findByTenantIdAndIdempotencyKey(normalizedTenantId, normalizedIdempotencyKey);
        if (existing.isPresent()) {
            return existing.get();
        }

        var subscription = PaymentSubscription.newPending(
                normalizedTenantId,
                purpose,
                amountMinor,
                normalizedCurrency,
                memberId,
                fundId,
                normalizedIdempotencyKey);
        subscription.setProvider("STRIPE");

        try {
            var session = stripeClient.createSubscriptionCheckoutSession(
                    subscription.getId().toString(),
                    normalizedTenantId,
                    amountMinor,
                    normalizedCurrency,
                    purpose.name(),
                    normalizedIdempotencyKey);
            subscription.attachCheckoutSession(session.getId(), session.getUrl());
        } catch (StripeException e) {
            log.warn("Stripe subscription session creation failed for tenant={} idempotencyKey={}: {}",
                    normalizedTenantId, normalizedIdempotencyKey, e.getMessage());
            throw new IllegalStateException("Stripe subscription session creation failed", e);
        }

        var saved = subscriptionRepository.save(subscription);
        outboxPublisher.publish(
                PaymentEventType.SUBSCRIPTION_INITIATED,
                saved.getId().toString(),
                saved.getTenantId(),
                Map.of(
                        "subscriptionId", saved.getId().toString(),
                        "purpose", saved.getPurpose().name(),
                        "amount", saved.getAmount().getAmount(),
                        "currency", saved.getAmount().getCurrency()
                )
        );

        return saved;
    }
}
