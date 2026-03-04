package com.anastasia.Anastasia_BackEnd.modules.registration.repository;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantSubscriptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface TenantSubscriptionRepository extends JpaRepository<TenantSubscriptionEntity, UUID> {

    @Query("""
        select ts
        from TenantSubscriptionEntity ts
        where ts.tenant.id = :tenantId
    """)
    Optional<TenantSubscriptionEntity> findByTenantId(@Param("tenantId") UUID tenantId);

    Optional<TenantSubscriptionEntity> findByProviderSubscriptionId(String providerSubscriptionId);
}
