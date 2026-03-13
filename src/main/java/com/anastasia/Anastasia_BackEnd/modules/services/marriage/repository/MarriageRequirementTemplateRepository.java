package com.anastasia.Anastasia_BackEnd.modules.services.marriage.repository;

import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriageRequirementTemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MarriageRequirementTemplateRepository extends JpaRepository<MarriageRequirementTemplateEntity, UUID> {
    List<MarriageRequirementTemplateEntity> findByChurch_ChurchIdAndEnabledTrueOrderByOrderIndexAsc(Long churchId);
    Optional<MarriageRequirementTemplateEntity> findByChurch_ChurchIdAndCode(Long churchId, String code);
}
