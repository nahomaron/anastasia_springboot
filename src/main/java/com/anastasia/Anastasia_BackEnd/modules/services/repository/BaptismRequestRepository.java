package com.anastasia.Anastasia_BackEnd.modules.services.repository;

import com.anastasia.Anastasia_BackEnd.modules.services.model.BaptismRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BaptismRequestRepository extends JpaRepository<BaptismRequestEntity, Long> {
    boolean existsByRequestNumber(String requestNumber);
    List<BaptismRequestEntity> findByRequestedByUser_UuidOrderByCreatedAtDesc(UUID userId);
    List<BaptismRequestEntity> findByTenantIdAndRequestedByUser_UuidOrderByCreatedAtDesc(UUID tenantId, UUID userId);
}
