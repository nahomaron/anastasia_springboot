package com.anastasia.Anastasia_BackEnd.modules.services.marriage.repository;

import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriagePartyDocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MarriagePartyDocumentRepository extends JpaRepository<MarriagePartyDocumentEntity, UUID> {
    List<MarriagePartyDocumentEntity> findByMarriageCaseId(UUID marriageCaseId);
    List<MarriagePartyDocumentEntity> findByPartyId(UUID partyId);
}
