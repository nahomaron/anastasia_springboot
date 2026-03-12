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
        name = "tenant_subscription_provider_links",
        indexes = {
                @Index(name = "idx_tenant_subscription_provider_links_subscription", columnList = "tenant_subscription_id"),
                @Index(name = "idx_tenant_subscription_provider_links_provider_sub", columnList = "provider, provider_subscription_id"),
                @Index(name = "idx_tenant_subscription_provider_links_active", columnList = "is_active")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_tenant_subscription_provider_links_provider_sub",
                        columnNames = {"provider", "provider_subscription_id"}
                )
        }
)
public class TenantSubscriptionProviderLinkEntity extends LocalDateTimeAuditMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_subscription_id", nullable = false)
    private TenantSubscriptionEntity tenantSubscription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private BillingProvider provider;

    @Column(name = "provider_customer_id", length = 128)
    private String providerCustomerId;

    @Column(name = "provider_subscription_id", length = 128)
    private String providerSubscriptionId;

    @Column(name = "provider_price_reference", length = 128)
    private String providerPriceReference;

    @Column(name = "provider_status", length = 64)
    private String providerStatus;

    @Column(name = "payment_method_last4", length = 4)
    private String paymentMethodLast4;

    @Column(name = "last_provider_event_id", length = 128)
    private String lastProviderEventId;

    @Column(name = "last_provider_event_type", length = 128)
    private String lastProviderEventType;

    @Column(name = "last_provider_event_at")
    private Instant lastProviderEventAt;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Version
    @Column(nullable = false)
    private long version;

    @PrePersist
    public void onCreate() {
        initializeAuditTimestamps(Instant.now());
    }

    @PreUpdate
    public void onUpdate() {
        touchAuditTimestamps(Instant.now());
    }
}
