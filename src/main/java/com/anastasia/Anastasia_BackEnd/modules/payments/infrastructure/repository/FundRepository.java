package com.anastasia.Anastasia_BackEnd.modules.payments.infrastructure.repository;

import com.anastasia.Anastasia_BackEnd.modules.payments.domain.model.FundEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FundRepository extends JpaRepository<FundEntity, UUID> {

    boolean existsByIdAndTenantId(UUID id, String tenantId);

    Optional<FundEntity> findByIdAndTenantId(UUID id, String tenantId);

    Optional<FundEntity> findByNameAndTenantId(String name, String tenantId);
}
