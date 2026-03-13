package com.anastasia.Anastasia_BackEnd.modules.services.marriage.repository;

import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriageCaseNoteEntity;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriageNoteVisibility;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MarriageCaseNoteRepository extends JpaRepository<MarriageCaseNoteEntity, UUID> {
    List<MarriageCaseNoteEntity> findByMarriageCaseIdOrderByCreatedAtDesc(UUID marriageCaseId);
    List<MarriageCaseNoteEntity> findByMarriageCaseIdAndVisibilityOrderByCreatedAtDesc(UUID marriageCaseId, MarriageNoteVisibility visibility);
}
