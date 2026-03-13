package com.anastasia.Anastasia_BackEnd.modules.services.marriage.repository;

import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriagePartySubmissionEntity;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriagePartySubmissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MarriagePartySubmissionRepository extends JpaRepository<MarriagePartySubmissionEntity, UUID> {
    List<MarriagePartySubmissionEntity> findByPartyIdOrderBySubmissionVersionDesc(UUID partyId);
    Optional<MarriagePartySubmissionEntity> findFirstByPartyIdAndStatusOrderBySubmissionVersionDesc(
            UUID partyId,
            MarriagePartySubmissionStatus status
    );
}
