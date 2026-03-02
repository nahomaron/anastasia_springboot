package com.anastasia.Anastasia_BackEnd.modules.registration.repository;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantPlanGrantEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TenantPlanGrantRepository extends JpaRepository<TenantPlanGrantEntity, UUID> {
    List<TenantPlanGrantEntity> findByTenant_IdOrderByCreatedAtDesc(UUID tenantId);
    List<TenantPlanGrantEntity> findByTenant_IdAndPromoCodeIgnoreCase(UUID tenantId, String promoCode);
}
