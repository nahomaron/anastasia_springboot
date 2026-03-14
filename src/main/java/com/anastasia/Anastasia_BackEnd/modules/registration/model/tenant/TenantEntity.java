package com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant;

import com.anastasia.Anastasia_BackEnd.modules.common.LocalDateTimeAuditMetadata;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "tenants",
        indexes = {
                @Index(name = "idx_tenants_status", columnList = "status"),
                @Index(name = "idx_tenants_owner_email", columnList = "owner_email"),
                @Index(name = "idx_tenants_deleted_at", columnList = "deleted_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_tenants_slug", columnNames = "slug")
        }
)
public class TenantEntity extends LocalDateTimeAuditMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 120)
    private String displayName;

    @Column(nullable = false, length = 120)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private TenantType tenantType; // CHURCH or PRIEST

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 32)
    private TenantStatus status = TenantStatus.DRAFT;

    @Column(nullable = false, length = 160)
    private String ownerName; // Can be a church name or a priest's full name

    @Column(name = "owner_email", nullable = false, length = 160)
    private String ownerEmail;

    @Column(name = "owner_phone", nullable = false, length = 64)
    private String phoneNumber; // Owner contact number

    @Builder.Default
    @Column(name = "phone_verified", nullable = false)
    private boolean phoneVerified = false;

    @Column(name = "phone_verified_at")
    private Instant phoneVerifiedAt;

    @Builder.Default
    @Column(name = "default_timezone", nullable = false, length = 64)
    private String defaultTimezone = "UTC";

    @Builder.Default
    @Column(name = "default_locale", nullable = false, length = 16)
    private String defaultLocale = "en";

    @Column(name = "country_code", length = 8)
    private String countryCode;

    @Column(name = "billing_email", length = 160)
    private String billingEmail;

    @Column(name = "external_id", length = 128)
    private String externalId;

    @Column(name = "source_system", length = 64)
    private String sourceSystem;

    @Column(name = "activated_at")
    private Instant activatedAt;

    @Column(name = "suspended_at")
    private Instant suspendedAt;

    @Column(name = "deactivated_at")
    private Instant deactivatedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "suspension_reason", length = 512)
    private String suspensionReason;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    @Column(nullable = false)
    private long version;

    @OneToOne(mappedBy = "tenant", cascade = CascadeType.ALL)
    private ChurchEntity church;

    @OneToOne(mappedBy = "tenant", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private TenantSubscriptionEntity subscription;

    @Builder.Default
    @OneToMany(mappedBy = "tenant", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<TenantAdminAssignmentEntity> adminAssignments = new HashSet<>();

    public void assignChurch(ChurchEntity church) {
        this.setChurch(church);
        church.setTenant(this);
    }

    public void assignSubscription(TenantSubscriptionEntity subscription) {
        this.subscription = subscription;
        if (subscription != null) {
            subscription.setTenant(this);
        }
    }

    public void addAdminAssignment(TenantAdminAssignmentEntity adminAssignment) {
        if (adminAssignment == null) {
            return;
        }
        adminAssignments.add(adminAssignment);
        adminAssignment.setTenant(this);
    }

    public void removeAdminAssignment(TenantAdminAssignmentEntity adminAssignment) {
        if (adminAssignment == null) {
            return;
        }
        adminAssignments.remove(adminAssignment);
        adminAssignment.setTenant(null);
    }

    @PrePersist
    public void onCreate() {
        Instant now = Instant.now();
        initializeAuditTimestamps(now);
        if (this.status == null) {
            this.status = TenantStatus.DRAFT;
        }
        if (this.defaultTimezone == null || this.defaultTimezone.isBlank()) {
            this.defaultTimezone = "UTC";
        }
        if (this.defaultLocale == null || this.defaultLocale.isBlank()) {
            this.defaultLocale = "en";
        }
    }

    @PreUpdate
    public void onUpdate() {
        touchAuditTimestamps(Instant.now());
    }

}
