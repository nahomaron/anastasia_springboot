package com.anastasia.Anastasia_BackEnd.modules.registration.repository;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntitlementAuditEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TenantEntitlementAuditRepository extends JpaRepository<TenantEntitlementAuditEntity, UUID> {
    List<TenantEntitlementAuditEntity> findByTenant_IdOrderByOccurredAtDesc(UUID tenantId);
}
