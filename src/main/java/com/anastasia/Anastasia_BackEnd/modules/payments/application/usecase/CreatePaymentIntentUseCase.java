package com.anastasia.Anastasia_BackEnd.modules.payments.application.usecase;

import com.anastasia.Anastasia_BackEnd.modules.payments.domain.model.PaymentIntent;
import com.anastasia.Anastasia_BackEnd.modules.payments.domain.model.PaymentPurpose;
import com.anastasia.Anastasia_BackEnd.modules.payments.infrastructure.repository.PaymentIntentRepository;
import com.anastasia.Anastasia_BackEnd.modules.payments.infrastructure.stripe.StripeClient;
import com.stripe.exception.StripeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreatePaymentIntentUseCase {
    private final PaymentIntentRepository repo;
    private final StripeClient stripeClient;

    @Transactional
    public PaymentIntent execute(String tenantId,
                                 PaymentPurpose purpose,
                                 long amountMinor,
                                 String currency,
                                 String memberId,
                                 String fundId,
                                 String idempotencyKey) {

        String normalizedTenantId = Objects.requireNonNull(tenantId, "tenantId must not be null").trim();
        String normalizedIdempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null").trim();
        String normalizedCurrency = Objects.requireNonNull(currency, "currency must not be null").trim().toUpperCase(Locale.ROOT);

        var existing = repo.findByTenantIdAndIdempotencyKey(normalizedTenantId, normalizedIdempotencyKey);
        if (existing.isPresent()) {
            return existing.get();
        }

        var intent = PaymentIntent.newInitiated(
                normalizedTenantId,
                purpose,
                amountMinor,
                normalizedCurrency,
                memberId,
                fundId,
                normalizedIdempotencyKey);
        intent.setProvider("STRIPE");

        try {
            var session = stripeClient.createCheckoutSession(
                    intent.getId().toString(),
                    normalizedTenantId,
                    amountMinor,
                    normalizedCurrency,
                    purpose.name(),
                    normalizedIdempotencyKey);
            intent.setProviderRef(session.getPaymentIntent());
            intent.setCheckoutUrl(session.getUrl());
        } catch (StripeException e) {
            intent.markFailed();
            repo.save(intent);
            log.warn("Stripe session creation failed for tenant={} idempotencyKey={}: {}", normalizedTenantId,
                    normalizedIdempotencyKey, e.getMessage());
            throw new IllegalStateException("Stripe session creation failed", e);
        }

        return repo.save(intent);
    }
}
