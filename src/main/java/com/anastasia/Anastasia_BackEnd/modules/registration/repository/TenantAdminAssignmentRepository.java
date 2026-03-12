package com.anastasia.Anastasia_BackEnd.modules.registration.repository;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.MembershipStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantAdminAssignmentEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantAdminAssignmentRepository extends JpaRepository<TenantAdminAssignmentEntity, UUID> {

    @Query("""
        select taa
        from TenantAdminAssignmentEntity taa
        where taa.tenant.id = :tenantId
          and taa.status = com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.MembershipStatus.ACTIVE
          and taa.role in :roles
    """)
    List<TenantAdminAssignmentEntity> findActiveUsersByTenantIdAndRoles(@Param("tenantId") UUID tenantId,
                                                                        @Param("roles") Collection<TenantRole> roles);

    @Query("""
        select (count(taa) > 0)
        from TenantAdminAssignmentEntity taa
        where taa.tenant.id = :tenantId
          and taa.userId = :userId
    """)
    boolean existsByTenantIdAndUserId(@Param("tenantId") UUID tenantId, @Param("userId") UUID userId);

    @Query("""
        select count(taa)
        from TenantAdminAssignmentEntity taa
        where taa.tenant.id = :tenantId
          and taa.role = :role
          and taa.status = :status
    """)
    long countByTenantIdAndRoleAndStatus(@Param("tenantId") UUID tenantId,
                                         @Param("role") TenantRole role,
                                         @Param("status") MembershipStatus status);

    Optional<TenantAdminAssignmentEntity> findByTenant_IdAndUserId(UUID tenantId, UUID userId);

    List<TenantAdminAssignmentEntity> findByTenant_IdOrderByCreatedAtAsc(UUID tenantId);

    List<TenantAdminAssignmentEntity> findByUserIdAndStatusAndTenant_IdNot(UUID userId, MembershipStatus status, UUID tenantId);
}
