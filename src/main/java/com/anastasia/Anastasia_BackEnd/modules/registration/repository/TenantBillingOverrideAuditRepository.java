package com.anastasia.Anastasia_BackEnd.modules.registration.repository;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantBillingOverrideAuditEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TenantBillingOverrideAuditRepository extends JpaRepository<TenantBillingOverrideAuditEntity, UUID> {

    List<TenantBillingOverrideAuditEntity> findByTenant_IdOrderByOccurredAtDesc(UUID tenantId);
}
