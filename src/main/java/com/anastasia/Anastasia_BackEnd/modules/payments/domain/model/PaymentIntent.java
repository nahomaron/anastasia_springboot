package com.anastasia.Anastasia_BackEnd.modules.payments.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_intents",
        uniqueConstraints = @UniqueConstraint(name = "usa_payment_intents_tenant_idempotency",
                columnNames = {"tenant_id", "idempotency_key"}))
@Getter @Setter
@NoArgsConstructor
public class PaymentIntent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable=false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    private PaymentPurpose purpose;

    @Embedded
    private Money amount;

    @Enumerated(EnumType.STRING) @Column(nullable=false)
    private PaymentStatus status;

    private String provider;        // e.g., STRIPE
    private String providerRef;     // e.g., Stripe PaymentIntent/Session id
    private Long memberId;
    private UUID userId;
    private String userEmail;
    private String fundId;
    private String fundName;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;
    private String checkoutUrl;

    private Instant createdAt;
    private Instant updatedAt;
    private Instant authorizedAt;
    private Instant capturedAt;
    private Long authorizedAmountMinor;
    private Long capturedGrossAmountMinor;
    private Long capturedFeeAmountMinor;
    private Long capturedNetAmountMinor;
    private String capturedCurrency;
    private String lastStripeEventId;
    private String lastStripeEventType;
    private Instant lastStripeEventReceivedAt;

    // Factory method to create a new initiated PaymentIntent
    // Note: does not persist to DB
    public static PaymentIntent newInitiated(UUID tenantId, PaymentPurpose purpose, long amt, String curr,
                                             Long memberId, UUID userId, String userEmail, String fundId, String idempotencyKey) {
        var pi = new PaymentIntent();
        pi.id = UUID.randomUUID();
        pi.tenantId = tenantId;
        pi.purpose = purpose;
        pi.amount = new Money(amt, curr);
        pi.status = PaymentStatus.INITIATED;
        pi.memberId = memberId;
        pi.userId = userId;
        pi.userEmail = userEmail != null && !userEmail.isBlank() ? userEmail.trim() : null;
        pi.fundId = fundId;
        pi.idempotencyKey = idempotencyKey;
        pi.createdAt = Instant.now();
        pi.updatedAt = pi.createdAt;
        return pi;
    }

    // State transition methods
    public void markAuthorized(String providerRef, Instant occurredAt, Long amountMinor) {
        ensureProviderReference(providerRef);
        if (status == PaymentStatus.CAPTURED || status == PaymentStatus.REFUNDED) {
            return;
        }
        if (status != PaymentStatus.AUTHORIZED) {
            this.status = PaymentStatus.AUTHORIZED;
        }
        this.authorizedAmountMinor = amountMinor;
        this.authorizedAt = occurredAt != null ? occurredAt : Instant.now();
        this.updatedAt = Instant.now();
    }

    // State transition methods
    public void markCaptured(String providerRef,
                             Long gross,
                             Long fees,
                             Long net,
                             String currency,
                             Instant occurredAt) {
        ensureProviderReference(providerRef);
        if (status == PaymentStatus.CAPTURED || status == PaymentStatus.REFUNDED) {
            return;
        }
        this.status = PaymentStatus.CAPTURED;
        this.capturedGrossAmountMinor = gross;
        this.capturedFeeAmountMinor = fees;
        this.capturedNetAmountMinor = net;
        if (currency != null && !currency.isBlank()) {
            this.capturedCurrency = currency;
        }
        this.capturedAt = occurredAt != null ? occurredAt : Instant.now();
        this.updatedAt = Instant.now();
    }

    public void markFailed() {
        if (status == PaymentStatus.CAPTURED || status == PaymentStatus.REFUNDED) {
            return;
        }
        this.status = PaymentStatus.FAILED;
        this.updatedAt = Instant.now();
    }

    public void recordStripeEvent(String eventId, String eventType, Instant receivedAt) {
        if (eventId != null && !eventId.isBlank()) {
            this.lastStripeEventId = eventId;
        }
        if (eventType != null && !eventType.isBlank()) {
            this.lastStripeEventType = eventType;
        }
        this.lastStripeEventReceivedAt = receivedAt != null ? receivedAt : Instant.now();
        this.updatedAt = Instant.now();
    }

    private void ensureProviderReference(String providerRef) {
        if (providerRef == null || providerRef.isBlank()) {
            throw new IllegalArgumentException("providerRef must be provided");
        }
        if (this.providerRef == null) {
            this.providerRef = providerRef;
        } else if (!this.providerRef.equals(providerRef)) {
            throw new IllegalStateException("PaymentIntent provider reference mismatch");
        }
    }
}
