package com.anastasia.Anastasia_BackEnd.modules.payments.application.usecase;

import com.anastasia.Anastasia_BackEnd.common.i18n.LocalizedMessageService;
import com.anastasia.Anastasia_BackEnd.modules.accounting.repository.FundRepository;
import com.anastasia.Anastasia_BackEnd.modules.payments.domain.model.PaymentIntent;
import com.anastasia.Anastasia_BackEnd.modules.payments.domain.model.PaymentPurpose;
import com.anastasia.Anastasia_BackEnd.modules.payments.repository.PaymentIntentRepository;
import com.anastasia.Anastasia_BackEnd.modules.payments.stripe.StripeClient;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.Adult_MemberEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.MemberRepository;
import com.stripe.exception.StripeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;


/**
 * Use case for creating a payment intent.
 * Creates a new payment intent or returns an existing one based on the idempotency key.
 * Validates member and fund references if provided.
 * Interacts with Stripe to create a checkout session.
 * Handles Stripe exceptions and marks the intent as failed if necessary.
 * Saves the payment intent to the repository.
 * Returns the created or existing payment intent.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CreatePaymentIntentUseCase {
    private final PaymentIntentRepository paymentIntentRepository;
    private final StripeClient stripeClient;
    private final MemberRepository memberRepo;
    private final FundRepository fundRepo;
    private final LocalizedMessageService messageService;

    // Creates or retrieves a payment intent based on the provided parameters.
    @Transactional
    public PaymentIntent execute(UUID tenantId,
                                 PaymentPurpose purpose,
                                 long amountMinor,
                                 String currency,
                                 Long memberId,
                                 UUID userId,
                                 String userEmail,
                                 String fundId,
                                 String idempotencyKey) {

        UUID normalizedTenantId = Objects.requireNonNull(tenantId, "tenantId must not be null");
        String normalizedIdempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null").trim();
        String normalizedCurrency = Objects.requireNonNull(currency, "currency must not be null").trim().toUpperCase(Locale.ROOT);

        var existing = paymentIntentRepository.findByTenantIdAndIdempotencyKey(normalizedTenantId, normalizedIdempotencyKey);
        if (existing.isPresent()) {
            return existing.get();
        }

        /**
         * Resolve memberId and userId:
         * - If memberId is provided, fetch the member and use its userId if userId is not provided.
         * - If only userId is provided, ensure the user belongs to the tenant and fetch the corresponding memberId if available.
         * - This ensures consistency and validity of references within the tenant context.
         * - AtomicReference is used for memberId to allow modification within the lambda expression.
         */
        UUID resolvedUserId = userId;
        AtomicReference<Long> resolvedMemberId = new AtomicReference<>(memberId);
        AtomicReference<String> resolvedUserEmail = new AtomicReference<>(userEmail);

        if (resolvedMemberId.get() != null) {
            Adult_MemberEntity member = memberRepo.findByIdAndTenantId(resolvedMemberId.get(), normalizedTenantId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            messageService.get(
                                    "payments.member.tenantMismatch",
                                    "Member does not belong to tenant: {0}",
                                    resolvedMemberId.get()
                            )));
            resolvedUserId = resolvedUserId != null ? resolvedUserId : member.getUserId();
            if (resolvedUserEmail.get() == null || resolvedUserEmail.get().isBlank()) {
                resolvedUserEmail.set(member.getEmail());
            }
        } else if (resolvedUserId != null) {
            // Ensure provided user belongs to tenant if possible
            memberRepo.findByUserIdAndTenantId(resolvedUserId, normalizedTenantId)
                    .ifPresent(found -> {
                        resolvedMemberId.set(found.getId());
                        if (resolvedUserEmail.get() == null || resolvedUserEmail.get().isBlank()) {
                            resolvedUserEmail.set(found.getEmail());
                        }
                    });
        }

        var intent = PaymentIntent.newInitiated(
                normalizedTenantId,
                purpose,
                amountMinor,
                normalizedCurrency,
                resolvedMemberId.get(),
                resolvedUserId,
                resolvedUserEmail.get(),
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
            intent.attachCheckoutSession(session.getId(), session.getUrl());
            if (session.getPaymentIntent() != null && !session.getPaymentIntent().isBlank()) {
                intent.setProviderPaymentReference(session.getPaymentIntent());
            }
        } catch (StripeException e) {
            intent.markFailed(e.getMessage());
            paymentIntentRepository.save(intent);
            log.warn("Stripe session creation failed for tenant={} idempotencyKey={}: {}", normalizedTenantId,
                    normalizedIdempotencyKey, e.getMessage());
            throw new IllegalStateException(messageService.get(
                    "payments.stripe.session.createFailed",
                    "Stripe session creation failed"
            ), e);
        }

        return paymentIntentRepository.save(intent);
    }

}
