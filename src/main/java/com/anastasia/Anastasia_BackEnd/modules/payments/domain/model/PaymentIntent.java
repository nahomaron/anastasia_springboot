package com.anastasia.Anastasia_BackEnd.modules.payments.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_intents",
        uniqueConstraints = @UniqueConstraint(name = "usa_payment_intents_tenant_idempotency",
                columnNames = {"tenant_id", "idempotency_key"}),
        indexes = {
                @Index(name = "idx_payment_intent_tenant_status_created", columnList = "tenant_id,status,created_at"),
                @Index(name = "idx_payment_intent_provider_payment_ref", columnList = "provider_payment_reference"),
                @Index(name = "idx_payment_intent_provider_checkout_ref", columnList = "provider_checkout_reference")
        })
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

    private String provider;

    @Column(name = "provider_payment_reference")
    private String providerPaymentReference;

    @Column(name = "provider_checkout_reference")
    private String providerCheckoutReference;

    private Long memberId;
    private UUID userId;
    private String userEmail;
    private String fundId;
    private String fundName;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;
    private String checkoutUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "status_changed_at")
    private Instant statusChangedAt;

    @Column(name = "status_reason", length = 512)
    private String statusReason;

    private Instant authorizedAt;
    private Instant capturedAt;
    private Instant failedAt;
    private Instant refundedAt;
    private Long authorizedAmountMinor;
    private Long capturedGrossAmountMinor;
    private Long capturedFeeAmountMinor;
    private Long capturedNetAmountMinor;
    private String capturedCurrency;

    private Long refundedAmountMinor;
    private String refundedCurrency;

    @Column(name = "provider_event_id")
    private String lastProviderEventId;

    @Column(name = "provider_event_type")
    private String lastProviderEventType;

    @Column(name = "provider_event_received_at")
    private Instant lastProviderEventReceivedAt;

    private Instant deletedAt;

    @Version
    @Column(nullable = false)
    private long version;

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
        pi.statusChangedAt = pi.createdAt;
        return pi;
    }

    public void attachCheckoutSession(String checkoutReference, String checkoutUrl) {
        if (checkoutReference == null || checkoutReference.isBlank()) {
            throw new IllegalArgumentException("checkoutReference must be provided");
        }
        if (this.providerCheckoutReference != null && !this.providerCheckoutReference.equals(checkoutReference)) {
            throw new IllegalStateException("PaymentIntent checkout reference mismatch");
        }
        this.providerCheckoutReference = checkoutReference;
        this.checkoutUrl = checkoutUrl;
        touch();
    }

    public void markAuthorized(String providerPaymentReference, Instant occurredAt, Long amountMinor) {
        ensureProviderPaymentReference(providerPaymentReference);
        if (status == PaymentStatus.CAPTURED || status == PaymentStatus.REFUNDED) {
            return;
        }
        transitionTo(PaymentStatus.AUTHORIZED, null, occurredAt);
        this.authorizedAmountMinor = amountMinor;
        this.authorizedAt = occurredAt != null ? occurredAt : Instant.now();
    }

    public void markCaptured(String providerPaymentReference,
                             Long gross,
                             Long fees,
                             Long net,
                             String currency,
                             Instant occurredAt) {
        ensureProviderPaymentReference(providerPaymentReference);
        if (status == PaymentStatus.CAPTURED || status == PaymentStatus.REFUNDED) {
            return;
        }
        transitionTo(PaymentStatus.CAPTURED, null, occurredAt);
        this.capturedGrossAmountMinor = gross;
        this.capturedFeeAmountMinor = fees;
        this.capturedNetAmountMinor = net;
        if (currency != null && !currency.isBlank()) {
            this.capturedCurrency = currency;
        }
        this.capturedAt = occurredAt != null ? occurredAt : Instant.now();
    }

    public void markFailed(String reason) {
        if (status == PaymentStatus.CAPTURED || status == PaymentStatus.REFUNDED) {
            return;
        }
        Instant now = Instant.now();
        transitionTo(PaymentStatus.FAILED, reason, now);
        this.failedAt = now;
    }

    public void markRefunded(Long amountMinor, String currency, Instant occurredAt, String reason) {
        Instant effectiveAt = occurredAt != null ? occurredAt : Instant.now();
        transitionTo(PaymentStatus.REFUNDED, reason, effectiveAt);
        this.refundedAmountMinor = amountMinor;
        if (currency != null && !currency.isBlank()) {
            this.refundedCurrency = currency;
        }
        this.refundedAt = effectiveAt;
    }

    public void recordProviderEvent(String eventId, String eventType, Instant receivedAt) {
        if (eventId != null && !eventId.isBlank()) {
            this.lastProviderEventId = eventId;
        }
        if (eventType != null && !eventType.isBlank()) {
            this.lastProviderEventType = eventType;
        }
        this.lastProviderEventReceivedAt = receivedAt != null ? receivedAt : Instant.now();
        touch();
    }

    private void ensureProviderPaymentReference(String providerPaymentReference) {
        if (providerPaymentReference == null || providerPaymentReference.isBlank()) {
            throw new IllegalArgumentException("providerPaymentReference must be provided");
        }
        if (this.providerPaymentReference == null) {
            this.providerPaymentReference = providerPaymentReference;
        } else if (!this.providerPaymentReference.equals(providerPaymentReference)) {
            throw new IllegalStateException("PaymentIntent provider reference mismatch");
        }
    }

    private void transitionTo(PaymentStatus nextStatus, String reason, Instant occurredAt) {
        this.status = nextStatus;
        this.statusReason = reason != null && !reason.isBlank() ? reason.trim() : null;
        this.statusChangedAt = occurredAt != null ? occurredAt : Instant.now();
        this.updatedAt = Instant.now();
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }

    public String getProviderRef() {
        return providerPaymentReference;
    }

    public void setProviderRef(String providerRef) {
        this.providerPaymentReference = providerRef;
    }

    public String getLastStripeEventId() {
        return lastProviderEventId;
    }

    public void setLastStripeEventId(String eventId) {
        this.lastProviderEventId = eventId;
    }

    public String getLastStripeEventType() {
        return lastProviderEventType;
    }

    public void setLastStripeEventType(String eventType) {
        this.lastProviderEventType = eventType;
    }

    public Instant getLastStripeEventReceivedAt() {
        return lastProviderEventReceivedAt;
    }

    public void setLastStripeEventReceivedAt(Instant receivedAt) {
        this.lastProviderEventReceivedAt = receivedAt;
    }
}
