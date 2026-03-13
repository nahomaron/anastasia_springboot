package com.anastasia.Anastasia_BackEnd.modules.services.marriage.repository;

import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriageConfessorApprovalEntity;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriageConfessorApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MarriageConfessorApprovalRepository extends JpaRepository<MarriageConfessorApprovalEntity, UUID> {
    List<MarriageConfessorApprovalEntity> findByMarriageCaseIdOrderByApprovalDateDesc(UUID marriageCaseId);
    List<MarriageConfessorApprovalEntity> findByMarriageCaseIdAndApprovalStatus(UUID marriageCaseId, MarriageConfessorApprovalStatus approvalStatus);
}
