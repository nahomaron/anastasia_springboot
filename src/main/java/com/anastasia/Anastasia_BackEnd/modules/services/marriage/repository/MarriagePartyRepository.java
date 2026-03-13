package com.anastasia.Anastasia_BackEnd.modules.services.marriage.repository;

import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriagePartyEntity;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriagePartyRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MarriagePartyRepository extends JpaRepository<MarriagePartyEntity, UUID> {
    List<MarriagePartyEntity> findByMarriageCaseId(UUID marriageCaseId);
    Optional<MarriagePartyEntity> findByMarriageCaseIdAndPartyRole(UUID marriageCaseId, MarriagePartyRole partyRole);
    boolean existsByMarriageCaseIdAndPartyRole(UUID marriageCaseId, MarriagePartyRole partyRole);
}
