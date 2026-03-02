package com.anastasia.Anastasia_BackEnd.modules.registration.repository;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.MembershipStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantRole;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantUserRepository extends JpaRepository<TenantUserEntity, UUID> {

    @Query("""
        select tu
        from TenantUserEntity tu
        where tu.tenant.id = :tenantId
          and tu.status = com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.MembershipStatus.ACTIVE
          and tu.role in :roles
    """)
    List<TenantUserEntity> findActiveUsersByTenantIdAndRoles(@Param("tenantId") UUID tenantId,
                                                              @Param("roles") Collection<TenantRole> roles);

    @Query("""
        select (count(tu) > 0)
        from TenantUserEntity tu
        where tu.tenant.id = :tenantId
          and tu.userId = :userId
    """)
    boolean existsByTenantIdAndUserId(@Param("tenantId") UUID tenantId, @Param("userId") UUID userId);

    @Query("""
        select count(tu)
        from TenantUserEntity tu
        where tu.tenant.id = :tenantId
          and tu.role = :role
          and tu.status = :status
    """)
    long countByTenantIdAndRoleAndStatus(@Param("tenantId") UUID tenantId,
                                         @Param("role") TenantRole role,
                                         @Param("status") MembershipStatus status);

    Optional<TenantUserEntity> findByTenant_IdAndUserId(UUID tenantId, UUID userId);

    List<TenantUserEntity> findByTenant_IdOrderByCreatedAtAsc(UUID tenantId);

    List<TenantUserEntity> findByUserIdAndStatusAndTenant_IdNot(UUID userId, MembershipStatus status, UUID tenantId);
}
