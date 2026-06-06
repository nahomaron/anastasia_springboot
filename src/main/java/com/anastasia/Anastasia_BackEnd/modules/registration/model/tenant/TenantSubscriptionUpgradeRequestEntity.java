package com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant;

import com.anastasia.Anastasia_BackEnd.modules.common.LocalDateTimeAuditMetadata;
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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "tenant_subscription_upgrade_requests",
        indexes = {
                @Index(name = "idx_tenant_subscription_upgrade_requests_subscription", columnList = "tenant_subscription_id"),
                @Index(name = "idx_tenant_subscription_upgrade_requests_status", columnList = "status"),
                @Index(name = "idx_tenant_subscription_upgrade_requests_checkout", columnList = "provider_checkout_session_id"),
                @Index(name = "idx_tenant_subscription_upgrade_requests_provider_sub", columnList = "provider, provider_subscription_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_tenant_subscription_upgrade_requests_idempotency",
                        columnNames = {"idempotency_key"}
                )
        }
)
public class TenantSubscriptionUpgradeRequestEntity extends LocalDateTimeAuditMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_subscription_id", nullable = false)
    private TenantSubscriptionEntity tenantSubscription;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_plan", nullable = false, length = 32)
    private SubscriptionPlan currentPlan;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_plan", nullable = false, length = 32)
    private SubscriptionPlan targetPlan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private BillingProvider provider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TenantSubscriptionUpgradeStatus status;

    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    @Column(name = "currency", length = 8)
    private String currency;

    @Column(name = "expected_amount_minor")
    private Long expectedAmountMinor;

    @Column(name = "provider_checkout_session_id", length = 128)
    private String providerCheckoutSessionId;

    @Column(name = "provider_customer_id", length = 128)
    private String providerCustomerId;

    @Column(name = "provider_subscription_id", length = 128)
    private String providerSubscriptionId;

    @Column(name = "provider_price_reference", length = 128)
    private String providerPriceReference;

    @Column(name = "checkout_url")
    private String checkoutUrl;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "checkout_completed_at")
    private Instant checkoutCompletedAt;

    @Column(name = "payment_confirmed_at")
    private Instant paymentConfirmedAt;

    @Column(name = "failure_reason", length = 512)
    private String failureReason;

    @Column(name = "created_by_user_id")
    private UUID createdByUserId;

    @Column(name = "updated_by_user_id")
    private UUID updatedByUserId;

    @Version
    @Column(nullable = false)
    private long version;

    @PrePersist
    public void onCreate() {
        Instant now = Instant.now();
        initializeAuditTimestamps(now);
        if (status == null) {
            status = TenantSubscriptionUpgradeStatus.PENDING_CHECKOUT;
        }
        if (provider == null) {
            provider = BillingProvider.STRIPE;
        }
    }

    @PreUpdate
    public void onUpdate() {
        touchAuditTimestamps(Instant.now());
    }
}
