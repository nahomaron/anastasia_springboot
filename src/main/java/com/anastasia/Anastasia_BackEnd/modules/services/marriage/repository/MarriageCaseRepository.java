package com.anastasia.Anastasia_BackEnd.modules.services.marriage.repository;

import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriageCaseEntity;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriageCaseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MarriageCaseRepository extends JpaRepository<MarriageCaseEntity, UUID> {
    boolean existsByCaseReference(String caseReference);
    Optional<MarriageCaseEntity> findByIdAndTenantId(UUID id, UUID tenantId);
    List<MarriageCaseEntity> findByTenantIdAndStatus(UUID tenantId, MarriageCaseStatus status);
    List<MarriageCaseEntity> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
    List<MarriageCaseEntity> findByChurchIdAndStatus(Long churchId, MarriageCaseStatus status);

    @Query("""
            SELECT DISTINCT mc
            FROM MarriageCaseEntity mc
            JOIN mc.parties p
            WHERE p.linkedUser.uuid = :userId
            ORDER BY mc.createdAt DESC
            """)
    List<MarriageCaseEntity> findVisibleForUser(@Param("userId") UUID userId);
}
