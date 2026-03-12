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
                columnNames = {"tenant_id", "idempotency_key"}),
        indexes = {
                @Index(name = "idx_payment_subscription_tenant_status_created", columnList = "tenant_id,status,created_at"),
                @Index(name = "idx_payment_subscription_provider_subscription_ref", columnList = "provider_subscription_reference"),
                @Index(name = "idx_payment_subscription_provider_checkout_ref", columnList = "provider_checkout_reference")
        })
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
    @Column(name = "user_id")
    private UUID userId;
    @Column(name = "user_email")
    private String userEmail;
    private String fundId;

    private String provider;

    @Column(name = "provider_subscription_reference")
    private String providerSubscriptionReference;

    @Column(name = "provider_checkout_reference")
    private String providerCheckoutReference;

    private String checkoutUrl;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "status_changed_at")
    private Instant statusChangedAt;

    @Column(name = "status_reason", length = 512)
    private String statusReason;

    private Instant activatedAt;
    private Instant canceledAt;
    private Instant deactivatedAt;

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

    public static PaymentSubscription newPending(UUID tenantId,
                                                 PaymentPurpose purpose,
                                                 long amountMinor,
                                                 String currency,
                                                 String memberId,
                                                 UUID userId,
                                                 String userEmail,
                                                 String fundId,
                                                 String idempotencyKey) {
        var subscription = new PaymentSubscription();
        subscription.id = UUID.randomUUID();
        subscription.tenantId = tenantId;
        subscription.purpose = purpose;
        subscription.amount = new Money(amountMinor, currency);
        subscription.status = SubscriptionStatus.PENDING;
        subscription.memberId = memberId;
        subscription.userId = userId;
        subscription.userEmail = userEmail != null && !userEmail.isBlank() ? userEmail.trim() : null;
        subscription.fundId = fundId;
        subscription.idempotencyKey = idempotencyKey;
        subscription.createdAt = Instant.now();
        subscription.updatedAt = subscription.createdAt;
        subscription.statusChangedAt = subscription.createdAt;
        return subscription;
    }

    public void attachCheckoutSession(String sessionId, String checkoutUrl) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("checkoutReference must be provided");
        }
        if (this.providerCheckoutReference != null && !this.providerCheckoutReference.equals(sessionId)) {
            throw new IllegalStateException("Subscription checkout reference mismatch");
        }
        this.providerCheckoutReference = sessionId;
        this.checkoutUrl = checkoutUrl;
        touch();
    }

    public void markActive(String providerSubscriptionReference) {
        if (providerSubscriptionReference == null || providerSubscriptionReference.isBlank()) {
            throw new IllegalArgumentException("providerSubscriptionReference must be provided");
        }
        if (this.providerSubscriptionReference != null && !this.providerSubscriptionReference.equals(providerSubscriptionReference)) {
            throw new IllegalStateException("Subscription provider reference mismatch");
        }
        this.providerSubscriptionReference = providerSubscriptionReference;
        transitionTo(SubscriptionStatus.ACTIVE, null, Instant.now());
        this.activatedAt = Instant.now();
        this.deactivatedAt = null;
    }

    public void markCanceled(String reason) {
        Instant now = Instant.now();
        transitionTo(SubscriptionStatus.CANCELED, reason, now);
        this.canceledAt = now;
        this.deactivatedAt = now;
    }

    public void markInactive(String reason) {
        Instant now = Instant.now();
        transitionTo(SubscriptionStatus.INACTIVE, reason, now);
        this.deactivatedAt = now;
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

    private void transitionTo(SubscriptionStatus nextStatus, String reason, Instant occurredAt) {
        this.status = nextStatus;
        this.statusReason = reason != null && !reason.isBlank() ? reason.trim() : null;
        this.statusChangedAt = occurredAt != null ? occurredAt : Instant.now();
        this.updatedAt = Instant.now();
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }

    public String getProviderRef() {
        return providerSubscriptionReference;
    }

    public void setProviderRef(String providerRef) {
        this.providerSubscriptionReference = providerRef;
    }

    public String getCheckoutSessionId() {
        return providerCheckoutReference;
    }

    public void setCheckoutSessionId(String checkoutSessionId) {
        this.providerCheckoutReference = checkoutSessionId;
    }
}
