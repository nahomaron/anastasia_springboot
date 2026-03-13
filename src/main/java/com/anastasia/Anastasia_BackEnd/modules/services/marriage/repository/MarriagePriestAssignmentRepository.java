package com.anastasia.Anastasia_BackEnd.modules.services.marriage.repository;

import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriagePriestAssignmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MarriagePriestAssignmentRepository extends JpaRepository<MarriagePriestAssignmentEntity, UUID> {
    List<MarriagePriestAssignmentEntity> findByMarriageCaseIdOrderByAssignedAtDesc(UUID marriageCaseId);
    Optional<MarriagePriestAssignmentEntity> findFirstByMarriageCaseIdAndActiveTrue(UUID marriageCaseId);
}
