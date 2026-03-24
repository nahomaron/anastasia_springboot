package com.anastasia.Anastasia_BackEnd.modules.registration.repository;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantFeatureOverrideEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantFeature;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TenantFeatureOverrideRepository extends JpaRepository<TenantFeatureOverrideEntity, UUID> {
    List<TenantFeatureOverrideEntity> findByTenant_IdOrderByCreatedAtDesc(UUID tenantId);
    List<TenantFeatureOverrideEntity> findByTenant_IdAndPromoCodeIgnoreCase(UUID tenantId, String promoCode);
    List<TenantFeatureOverrideEntity> findByTenant_IdAndFeatureOrderByCreatedAtDesc(UUID tenantId, TenantFeature feature);
}
