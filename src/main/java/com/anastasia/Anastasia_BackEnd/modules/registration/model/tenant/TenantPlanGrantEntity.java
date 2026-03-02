package com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant;

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
@Table(name = "tenant_plan_grants", indexes = {
        @Index(name = "idx_tenant_plan_grants_tenant", columnList = "tenant_id"),
        @Index(name = "idx_tenant_plan_grants_active", columnList = "active"),
        @Index(name = "idx_tenant_plan_grants_expires", columnList = "expires_at")
})
public class TenantPlanGrantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private TenantEntity tenant;

    @Enumerated(EnumType.STRING)
    @Column(name = "granted_plan", nullable = false, length = 32)
    private SubscriptionPlan grantedPlan;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 16)
    private GrantSource source;

    @Column(name = "promo_code", length = 64)
    private String promoCode;

    @Column(name = "active_member_limit_override")
    private Integer activeMemberLimitOverride;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "reason", length = 1024)
    private String reason;

    @Column(name = "created_by_user_id")
    private UUID createdByUserId;

    @Column(name = "updated_by_user_id")
    private UUID updatedByUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.startsAt == null) {
            this.startsAt = now;
        }
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isEffective(LocalDateTime now) {
        if (!active) {
            return false;
        }
        if (startsAt != null && startsAt.isAfter(now)) {
            return false;
        }
        return expiresAt == null || expiresAt.isAfter(now);
    }
}
