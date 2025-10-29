package com.anastasia.Anastasia_BackEnd.modules.payments.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_subscriptions",
        uniqueConstraints = @UniqueConstraint(name = "uk_payment_subscriptions_tenant_idempotency",
                columnNames = {"tenant_id", "idempotency_key"}))
@Getter
@Setter
@NoArgsConstructor
public class PaymentSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentPurpose purpose;

    @Embedded
    private Money amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionStatus status;

    private String memberId;
    private String fundId;

    private String provider;
    private String providerRef; // Stripe subscription id
    private String checkoutSessionId;
    private String checkoutUrl;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    private Instant createdAt;
    private Instant updatedAt;

    public static PaymentSubscription newPending(UUID tenantId,
                                                 PaymentPurpose purpose,
                                                 long amountMinor,
                                                 String currency,
                                                 String memberId,
                                                 String fundId,
                                                 String idempotencyKey) {
        var subscription = new PaymentSubscription();
        subscription.id = UUID.randomUUID();
        subscription.tenantId = tenantId;
        subscription.purpose = purpose;
        subscription.amount = new Money(amountMinor, currency);
        subscription.status = SubscriptionStatus.PENDING;
        subscription.memberId = memberId;
        subscription.fundId = fundId;
        subscription.idempotencyKey = idempotencyKey;
        subscription.createdAt = Instant.now();
        subscription.updatedAt = subscription.createdAt;
        return subscription;
    }

    public void attachCheckoutSession(String sessionId, String checkoutUrl) {
        this.checkoutSessionId = sessionId;
        this.checkoutUrl = checkoutUrl;
        this.updatedAt = Instant.now();
    }

    public void markActive(String providerRef) {
        if (providerRef == null || providerRef.isBlank()) {
            throw new IllegalArgumentException("providerRef must be provided");
        }
        if (this.providerRef != null && !this.providerRef.equals(providerRef)) {
            throw new IllegalStateException("Subscription provider reference mismatch");
        }
        this.providerRef = providerRef;
        this.status = SubscriptionStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public void markCanceled() {
        this.status = SubscriptionStatus.CANCELED;
        this.updatedAt = Instant.now();
    }
}
