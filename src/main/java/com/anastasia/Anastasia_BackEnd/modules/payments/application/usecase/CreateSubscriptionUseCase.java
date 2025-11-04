package com.anastasia.Anastasia_BackEnd.modules.payments.application.usecase;

import com.anastasia.Anastasia_BackEnd.modules.payments.domain.events.PaymentEventType;
import com.anastasia.Anastasia_BackEnd.modules.payments.domain.model.PaymentPurpose;
import com.anastasia.Anastasia_BackEnd.modules.payments.domain.model.PaymentSubscription;
import com.anastasia.Anastasia_BackEnd.core.outbox.OutboxPublisher;
import com.anastasia.Anastasia_BackEnd.modules.payments.repository.PaymentSubscriptionRepository;
import com.anastasia.Anastasia_BackEnd.modules.payments.stripe.StripeClient;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.MemberRepository;
import com.stripe.exception.StripeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreateSubscriptionUseCase {

    private final PaymentSubscriptionRepository subscriptionRepository;
    private final StripeClient stripeClient;
    private final OutboxPublisher outboxPublisher;
    private final MemberRepository memberRepository;

    @Transactional
    public PaymentSubscription execute(UUID tenantId,
                                       PaymentPurpose purpose,
                                       long amountMinor,
                                       String currency,
                                       String memberId,
                                       UUID userId,
                                       String userEmail,
                                       String fundId,
                                       String idempotencyKey) {

        UUID normalizedTenantId = Objects.requireNonNull(tenantId, "tenantId must not be null");
        String normalizedIdempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null").trim();
        String normalizedCurrency = Objects.requireNonNull(currency, "currency must not be null").trim().toUpperCase(Locale.ROOT);
        AtomicReference<String> resolvedMemberId = new AtomicReference<>(memberId != null && !memberId.isBlank() ? memberId.trim() : null);
        AtomicReference<UUID> resolvedUserId = new AtomicReference<>(userId);
        AtomicReference<String> resolvedUserEmail = new AtomicReference<>(userEmail != null && !userEmail.isBlank() ? userEmail.trim() : null);

        if (resolvedMemberId.get() != null) {
            try {
                Long memberPk = Long.valueOf(resolvedMemberId.get());
                memberRepository.findByIdAndTenantId(memberPk, normalizedTenantId)
                        .ifPresent(member -> {
                            if (member.getId() != null) {
                                resolvedMemberId.set(String.valueOf(member.getId()));
                            }
                            if (resolvedUserId.get() == null && member.getUserId() != null) {
                                resolvedUserId.set(member.getUserId());
                            }
                            if ((resolvedUserEmail.get() == null || resolvedUserEmail.get().isBlank()) && member.getEmail() != null
                                    && !member.getEmail().isBlank()) {
                                resolvedUserEmail.set(member.getEmail().trim());
                            }
                        });
            } catch (NumberFormatException ex) {
                log.debug("Unable to resolve member {} for tenant {}: {}", resolvedMemberId.get(), normalizedTenantId, ex.getMessage());
            }
        }

        if (resolvedUserId.get() != null) {
            memberRepository.findByUserIdAndTenantId(resolvedUserId.get(), normalizedTenantId)
                    .ifPresent(member -> {
                        if (resolvedMemberId.get() == null && member.getId() != null) {
                            resolvedMemberId.set(String.valueOf(member.getId()));
                        }
                        if ((resolvedUserEmail.get() == null || resolvedUserEmail.get().isBlank()) && member.getEmail() != null
                                && !member.getEmail().isBlank()) {
                            resolvedUserEmail.set(member.getEmail().trim());
                        }
                    });
        }

        var existing = subscriptionRepository.findByTenantIdAndIdempotencyKey(normalizedTenantId, normalizedIdempotencyKey);
        if (existing.isPresent()) {
            return existing.get();
        }

        var subscription = PaymentSubscription.newPending(
                normalizedTenantId,
                purpose,
                amountMinor,
                normalizedCurrency,
                resolvedMemberId.get(),
                resolvedUserId.get(),
                resolvedUserEmail.get(),
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
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("subscriptionId", saved.getId().toString());
        payload.put("purpose", saved.getPurpose().name());
        payload.put("amount", saved.getAmount().getAmount());
        payload.put("currency", saved.getAmount().getCurrency());
        payload.put("memberId", saved.getMemberId());
        payload.put("userId", saved.getUserId() != null ? saved.getUserId().toString() : null);
        payload.put("userEmail", saved.getUserEmail());
        payload.put("memberEmail", saved.getUserEmail());

        outboxPublisher.publish(
                PaymentEventType.SUBSCRIPTION_INITIATED,
                tenantId,
                saved.getId().toString(),
                payload
        );

        return saved;
    }
}
