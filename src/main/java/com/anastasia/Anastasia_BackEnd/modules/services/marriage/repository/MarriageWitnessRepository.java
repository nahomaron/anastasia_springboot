package com.anastasia.Anastasia_BackEnd.modules.services.marriage.repository;

import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriageWitnessEntity;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriageWitnessType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MarriageWitnessRepository extends JpaRepository<MarriageWitnessEntity, UUID> {
    List<MarriageWitnessEntity> findByMarriageCaseIdOrderBySortOrderAsc(UUID marriageCaseId);
    List<MarriageWitnessEntity> findByMarriageCaseIdAndWitnessTypeOrderBySortOrderAsc(UUID marriageCaseId, MarriageWitnessType witnessType);
}
