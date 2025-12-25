package com.anastasia.Anastasia_BackEnd.modules.accounting.repository;

import com.anastasia.Anastasia_BackEnd.modules.accounting.model.Fund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FundRepository extends JpaRepository<Fund, Long> {

    List<Fund> findByTenantId(UUID tenantId);

    Optional<Fund> findByIdAndTenantId(Long id, UUID tenantId);

    boolean existsByIdAndTenantId(Long id, UUID tenantId);

    boolean existsByTenantIdAndNameIgnoreCase(UUID tenantId, String name);
}
