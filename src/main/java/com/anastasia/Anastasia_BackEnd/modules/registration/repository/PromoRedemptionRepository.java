package com.anastasia.Anastasia_BackEnd.modules.registration.repository;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.PromoRedemptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PromoRedemptionRepository extends JpaRepository<PromoRedemptionEntity, UUID> {
    List<PromoRedemptionEntity> findByTenant_IdOrderByCreatedAtDesc(UUID tenantId);
    boolean existsByTenant_IdAndPromoCode_IdAndActiveTrue(UUID tenantId, UUID promoCodeId);
}
