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
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "tenant_billing_overrides", indexes = {
        @Index(name = "idx_tenant_billing_overrides_tenant", columnList = "tenant_id"),
        @Index(name = "idx_tenant_billing_overrides_active", columnList = "active"),
        @Index(name = "idx_tenant_billing_overrides_window", columnList = "starts_at, ends_at")
})
public class TenantBillingOverrideEntity extends LocalDateTimeAuditMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private TenantEntity tenant;

    @Enumerated(EnumType.STRING)
    @Column(name = "override_type", nullable = false, length = 32)
    private BillingOverrideType overrideType;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at")
    private Instant endsAt;

    @Column(name = "discount_percent", precision = 5, scale = 2)
    private BigDecimal discountPercent;

    @Column(name = "fixed_amount_minor")
    private Long fixedAmountMinor;

    @Column(name = "currency", length = 8)
    private String currency;

    @Column(name = "reason", length = 512)
    private String reason;

    @Column(name = "internal_note", length = 1024)
    private String internalNote;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "created_by_user_id")
    private UUID createdByUserId;

    @Column(name = "updated_by_user_id")
    private UUID updatedByUserId;

    @Column(name = "revoked_by_user_id")
    private UUID revokedByUserId;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    @Column(nullable = false)
    private long version;

    @PrePersist
    public void onCreate() {
        Instant now = Instant.now();
        initializeAuditTimestamps(now);
        if (this.startsAt == null) {
            this.startsAt = now;
        }
    }

    @PreUpdate
    public void onUpdate() {
        touchAuditTimestamps(Instant.now());
    }

    public boolean isEffective(Instant now) {
        if (!active || revokedAt != null || deletedAt != null) {
            return false;
        }
        if (startsAt != null && startsAt.isAfter(now)) {
            return false;
        }
        return endsAt == null || endsAt.isAfter(now);
    }
}
