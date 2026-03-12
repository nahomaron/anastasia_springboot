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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
        name = "tenant_admin_assignments",
        indexes = {
                @Index(name = "idx_tenant_admin_assignments_tenant_role_status", columnList = "tenant_id, role, status"),
                @Index(name = "idx_tenant_admin_assignments_user_status", columnList = "user_id, status")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_tenant_admin_assignments_user", columnNames = "user_id"),
                @UniqueConstraint(name = "uk_tenant_admin_assignments_tenant_user", columnNames = {"tenant_id", "user_id"})
        }
)
public class TenantAdminAssignmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    @JsonIgnore
    private TenantEntity tenant;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TenantRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private MembershipStatus status;

    @Builder.Default
    @Column(name = "is_billing_contact", nullable = false)
    private boolean isBillingContact = false;

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
            this.status = MembershipStatus.INVITED;
        }
        if (this.role == null) {
            this.role = TenantRole.COMMITTEE;
        }
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
