package com.anastasia.Anastasia_BackEnd.modules.registration.repository;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.SubscriptionPlanHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SubscriptionPlanHistoryRepository extends JpaRepository<SubscriptionPlanHistoryEntity, UUID> {
    List<SubscriptionPlanHistoryEntity> findTop20ByTenantIdOrderByEffectiveAtDesc(UUID tenantId);
}
