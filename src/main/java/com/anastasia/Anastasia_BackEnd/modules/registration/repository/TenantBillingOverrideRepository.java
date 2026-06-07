package com.anastasia.Anastasia_BackEnd.modules.registration.repository;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantBillingOverrideEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantBillingOverrideRepository extends JpaRepository<TenantBillingOverrideEntity, UUID> {

    List<TenantBillingOverrideEntity> findByTenant_IdOrderByCreatedAtDesc(UUID tenantId);

    @Query("""
        select o
        from TenantBillingOverrideEntity o
        where o.tenant.id = :tenantId
          and o.deletedAt is null
          and o.active = true
          and o.revokedAt is null
        order by o.startsAt desc, o.createdAt desc
    """)
    List<TenantBillingOverrideEntity> findActiveCandidatesByTenantId(@Param("tenantId") UUID tenantId);

    @Query("""
        select o
        from TenantBillingOverrideEntity o
        where o.id = :overrideId
          and o.tenant.id = :tenantId
          and o.deletedAt is null
    """)
    Optional<TenantBillingOverrideEntity> findByIdAndTenantId(@Param("overrideId") UUID overrideId,
                                                              @Param("tenantId") UUID tenantId);
}
