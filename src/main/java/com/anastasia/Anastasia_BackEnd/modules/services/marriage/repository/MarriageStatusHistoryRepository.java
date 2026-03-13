package com.anastasia.Anastasia_BackEnd.modules.services.marriage.repository;

import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriageStatusHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MarriageStatusHistoryRepository extends JpaRepository<MarriageStatusHistoryEntity, UUID> {
    List<MarriageStatusHistoryEntity> findByMarriageCaseIdOrderByChangedAtDesc(UUID marriageCaseId);
}
