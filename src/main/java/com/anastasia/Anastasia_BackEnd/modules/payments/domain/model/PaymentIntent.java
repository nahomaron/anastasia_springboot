package com.anastasia.Anastasia_BackEnd.modules.payments.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_intents")
@Getter @Setter
@NoArgsConstructor
public class PaymentIntent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable=false)
    private String tenantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    private PaymentPurpose purpose;

    @Embedded
    private Money amount;

    @Enumerated(EnumType.STRING) @Column(nullable=false)
    private PaymentStatus status;

    private String provider;        // e.g., STRIPE
    private String providerRef;     // e.g., Stripe PaymentIntent/Session id
    private String memberId;
    private String fundId;

    @Column(unique=true) private String idempotencyKey;
    private String checkoutUrl;

    private Instant createdAt;
    private Instant updatedAt;

    public static PaymentIntent newInitiated(String tenantId, PaymentPurpose purpose, long amt, String curr,
                                             String memberId, String fundId, String idempotencyKey) {
        var pi = new PaymentIntent();
        pi.id = UUID.randomUUID();
        pi.tenantId = tenantId;
        pi.purpose = purpose;
        pi.amount = new Money(amt, curr);
        pi.status = PaymentStatus.INITIATED;
        pi.memberId = memberId;
        pi.fundId = fundId;
        pi.idempotencyKey = idempotencyKey;
        pi.createdAt = Instant.now();
        pi.updatedAt = pi.createdAt;
        return pi;
    }

    public void markAuthorized(String providerRef) {
        this.providerRef = providerRef;
        this.status = PaymentStatus.AUTHORIZED;
        this.updatedAt = Instant.now();
    }

    public void markCaptured() {
        this.status = PaymentStatus.CAPTURED;
        this.updatedAt = Instant.now();
    }

    public void markFailed() {
        this.status = PaymentStatus.FAILED;
        this.updatedAt = Instant.now();
    }
}
