package com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "promo_codes", indexes = {
        @Index(name = "idx_promo_codes_code", columnList = "code", unique = true),
        @Index(name = "idx_promo_codes_active", columnList = "active"),
        @Index(name = "idx_promo_codes_expires", columnList = "expires_at")
})
public class PromoCodeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "code", nullable = false, unique = true, length = 64)
    private String code;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "description", length = 1024)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "granted_plan", length = 32)
    private SubscriptionPlan grantedPlan;

    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "promo_code_features", joinColumns = @JoinColumn(name = "promo_code_id"))
    @Column(name = "feature", nullable = false, length = 64)
    private Set<TenantFeature> grantedFeatures = new HashSet<>();

    @Column(name = "active_member_limit_override")
    private Integer activeMemberLimitOverride;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "max_redemptions")
    private Integer maxRedemptions;

    @Builder.Default
    @Column(name = "current_redemptions", nullable = false)
    private int currentRedemptions = 0;

    @Builder.Default
    @Column(name = "one_time_per_tenant", nullable = false)
    private boolean oneTimePerTenant = true;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

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
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
