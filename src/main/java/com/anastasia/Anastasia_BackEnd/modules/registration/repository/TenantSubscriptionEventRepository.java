package com.anastasia.Anastasia_BackEnd.modules.registration.repository;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantSubscriptionEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TenantSubscriptionEventRepository extends JpaRepository<TenantSubscriptionEventEntity, UUID> {

    @Query("""
        select e
        from TenantSubscriptionEventEntity e
        where e.tenant.id = :tenantId
        order by e.occurredAt desc
    """)
    List<TenantSubscriptionEventEntity> findByTenantIdOrderByOccurredAtDesc(@Param("tenantId") UUID tenantId);
}
