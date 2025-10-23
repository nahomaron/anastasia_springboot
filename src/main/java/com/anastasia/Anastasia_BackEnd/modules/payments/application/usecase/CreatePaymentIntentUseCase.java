package com.anastasia.Anastasia_BackEnd.modules.payments.application.usecase;

import com.anastasia.Anastasia_BackEnd.modules.payments.domain.model.*;
import com.anastasia.Anastasia_BackEnd.modules.payments.infrastructure.repository.PaymentIntentRepository;
import com.anastasia.Anastasia_BackEnd.modules.payments.infrastructure.stripe.StripeClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreatePaymentIntentUseCase {
    private final PaymentIntentRepository repo;
    private final StripeClient stripeClient;

    @Transactional
    public PaymentIntent execute(String tenantId, PaymentPurpose purpose, long amountMinor, String currency,
                                 String memberId, String fundId, String idempotencyKey) {

        // Idempotency: return existing if same key
        var existing = repo.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) return existing.get();

        var intent = PaymentIntent.newInitiated(tenantId, purpose, amountMinor, currency, memberId, fundId, idempotencyKey);
        intent.setProvider("STRIPE");

        // create checkout session on Stripe
        try {
            var session = stripeClient.createCheckoutSession(
                    intent.getId().toString(), amountMinor, currency, purpose.name());
            intent.setProviderRef(session.getId());
            intent.setCheckoutUrl(session.getUrl());
        } catch (Exception e) {
            intent.markFailed();
            repo.save(intent);
            throw new RuntimeException("Stripe session creation failed", e);
        }

        return repo.save(intent);
    }
}
