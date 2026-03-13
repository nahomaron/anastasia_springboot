package com.anastasia.Anastasia_BackEnd.modules.services.marriage.repository;

import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriageRequirementAssignmentEntity;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriageRequirementStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MarriageRequirementAssignmentRepository extends JpaRepository<MarriageRequirementAssignmentEntity, UUID> {
    List<MarriageRequirementAssignmentEntity> findByMarriageCaseId(UUID marriageCaseId);
    List<MarriageRequirementAssignmentEntity> findByPartyId(UUID partyId);
    List<MarriageRequirementAssignmentEntity> findByMarriageCaseIdAndCurrentStatus(UUID marriageCaseId, MarriageRequirementStatus status);
}
