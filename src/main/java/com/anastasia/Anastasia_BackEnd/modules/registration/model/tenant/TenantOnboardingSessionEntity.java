package com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant;

import com.anastasia.Anastasia_BackEnd.modules.common.LocalDateTimeAuditMetadata;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
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
        name = "tenant_onboarding_sessions",
        indexes = {
                @Index(name = "idx_tenant_onboarding_sessions_status", columnList = "status"),
                @Index(name = "idx_tenant_onboarding_sessions_owner_email", columnList = "owner_email"),
                @Index(name = "idx_tenant_onboarding_sessions_expires", columnList = "expires_at")
        }
)
public class TenantOnboardingSessionEntity extends LocalDateTimeAuditMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 128)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private OnboardingSessionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "tenant_type", nullable = false, length = 24)
    private TenantType tenantType;

    @Enumerated(EnumType.STRING)
    @Column(name = "selected_plan", nullable = false, length = 32)
    private SubscriptionPlan selectedPlan;

    @Column(name = "owner_name", nullable = false)
    private String ownerName;

    @Column(name = "owner_email", nullable = false)
    private String ownerEmail;

    @Column(name = "owner_phone", nullable = false, length = 64)
    private String ownerPhone;

    @Builder.Default
    @Column(name = "terms_accepted", nullable = false)
    private boolean termsAccepted = false;

    @Column(name = "terms_accepted_at")
    private Instant termsAcceptedAt;

    @Column(name = "terms_version", length = 32)
    private String termsVersion;

    @Lob
    @Column(name = "draft_payload_json", nullable = false)
    private String draftPayloadJson;

    @Column(name = "draft_password_hash", nullable = false)
    private String draftPasswordHash;

    @Column(name = "access_token_hash", length = 64)
    private String accessTokenHash;

    @Builder.Default
    @Column(name = "payment_required", nullable = false)
    private boolean paymentRequired = true;

    @Column(length = 12)
    private String currency;

    @Column(name = "expected_amount_minor")
    private Long expectedAmountMinor;

    @Lob
    @Column(name = "checkout_url")
    private String checkoutUrl;

    @Column(name = "provider_checkout_session_id", length = 128)
    private String providerCheckoutSessionId;

    @Column(name = "provider_subscription_id", length = 128)
    private String providerSubscriptionId;

    @Column(name = "provider_customer_id", length = 128)
    private String providerCustomerId;

    @Column(name = "checkout_created_at")
    private Instant checkoutCreatedAt;

    @Column(name = "payment_confirmed_at")
    private Instant paymentConfirmedAt;

    @Column(name = "provisioned_tenant_id")
    private UUID provisionedTenantId;

    @Column(name = "provisioned_owner_user_id")
    private UUID provisionedOwnerUserId;

    @Column(name = "provisioned_at")
    private Instant provisionedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Lob
    @Column(name = "failure_reason")
    private String failureReason;

    @PrePersist
    public void onCreate() {
        Instant now = Instant.now();
        initializeAuditTimestamps(now);
        if (this.status == null) {
            this.status = OnboardingSessionStatus.DRAFT;
        }
    }

    @PreUpdate
    public void onUpdate() {
        touchAuditTimestamps(Instant.now());
    }
}
