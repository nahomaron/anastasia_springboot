package com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "tenant_subscriptions",
        indexes = {
                @Index(name = "idx_tenant_subscriptions_tenant", columnList = "tenant_id"),
                @Index(name = "idx_tenant_subscriptions_status", columnList = "status"),
                @Index(name = "idx_tenant_subscriptions_provider_subscription", columnList = "provider_subscription_id")
        }
)
public class TenantSubscriptionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false, unique = true)
    @JsonIgnore
    private TenantEntity tenant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SubscriptionPlan plan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private SubscriptionStatus status;

    @Column(name = "trial_start_at")
    private LocalDateTime trialStartAt;

    @Column(name = "trial_end_at")
    private LocalDateTime trialEndAt;

    @Column(name = "current_period_start_at")
    private LocalDateTime currentPeriodStartAt;

    @Column(name = "current_period_end_at")
    private LocalDateTime currentPeriodEndAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_interval", nullable = false, length = 16)
    private BillingInterval billingInterval;

    @Builder.Default
    @Column(name = "cancel_at_period_end", nullable = false)
    private boolean cancelAtPeriodEnd = false;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private BillingProvider provider;

    @Column(name = "provider_customer_id")
    private String providerCustomerId;

    @Column(name = "provider_subscription_id")
    private String providerSubscriptionId;

    @Column(name = "stripe_price_id")
    private String stripePriceId;

    @Column(name = "last_stripe_event_id")
    private String lastStripeEventId;

    @Column(name = "last_stripe_event_at")
    private LocalDateTime lastStripeEventAt;

    @Column(name = "last_payment_at")
    private LocalDateTime lastPaymentAt;

    @Column(name = "grace_period_ends_at")
    private LocalDateTime gracePeriodEndsAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "pending_plan", length = 32)
    private SubscriptionPlan pendingPlan;

    @Column(name = "pending_plan_effective_at")
    private LocalDateTime pendingPlanEffectiveAt;

    @Column(name = "payment_method_last4", length = 4)
    private String paymentMethodLast4;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by_user_id")
    private UUID createdByUserId;

    @Column(name = "updated_by_user_id")
    private UUID updatedByUserId;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = SubscriptionStatus.TRIALING;
        }
        if (this.provider == null) {
            this.provider = BillingProvider.MANUAL;
        }
        if (this.billingInterval == null) {
            this.billingInterval = BillingInterval.MONTHLY;
        }
        if (this.plan == null) {
            this.plan = SubscriptionPlan.FREE;
        }
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
