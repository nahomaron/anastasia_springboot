package com.anastasia.Anastasia_BackEnd.modules.services.marriage.repository;

import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriageImpedimentEntity;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriageImpedimentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MarriageImpedimentRepository extends JpaRepository<MarriageImpedimentEntity, UUID> {
    List<MarriageImpedimentEntity> findByMarriageCaseId(UUID marriageCaseId);
    List<MarriageImpedimentEntity> findByMarriageCaseIdAndStatus(UUID marriageCaseId, MarriageImpedimentStatus status);
}
