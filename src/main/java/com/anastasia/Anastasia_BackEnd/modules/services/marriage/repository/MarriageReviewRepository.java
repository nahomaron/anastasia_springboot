package com.anastasia.Anastasia_BackEnd.modules.services.marriage.repository;

import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriageReviewEntity;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriageReviewStage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MarriageReviewRepository extends JpaRepository<MarriageReviewEntity, UUID> {
    List<MarriageReviewEntity> findByMarriageCaseIdOrderByReviewedAtDesc(UUID marriageCaseId);
    List<MarriageReviewEntity> findByMarriageCaseIdAndStageOrderByReviewedAtDesc(UUID marriageCaseId, MarriageReviewStage stage);
}
