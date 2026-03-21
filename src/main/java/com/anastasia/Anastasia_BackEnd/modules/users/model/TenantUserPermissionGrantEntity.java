package com.anastasia.Anastasia_BackEnd.modules.users.model;

import com.anastasia.Anastasia_BackEnd.core.auth.permission.Permission;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
        name = "tenant_user_permission_grants",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_tenant_user_permission_grants_user_tenant_permission",
                        columnNames = {"user_id", "tenant_id", "permission_id"}
                )
        },
        indexes = {
                @Index(name = "idx_tenant_user_permission_grants_user_tenant", columnList = "user_id, tenant_id"),
                @Index(name = "idx_tenant_user_permission_grants_tenant", columnList = "tenant_id")
        }
)
public class TenantUserPermissionGrantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "user_id", insertable = false, updatable = false)
    private UUID userId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private TenantEntity tenant;

    @Column(name = "tenant_id", insertable = false, updatable = false)
    private UUID tenantId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "permission_id", nullable = false)
    private Permission permission;

    @Column(name = "granted_by_user_id")
    private UUID grantedByUserId;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
