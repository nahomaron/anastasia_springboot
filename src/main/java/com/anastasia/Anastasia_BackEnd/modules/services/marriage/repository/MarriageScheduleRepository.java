package com.anastasia.Anastasia_BackEnd.modules.services.marriage.repository;

import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriageScheduleEntity;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriageScheduleStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MarriageScheduleRepository extends JpaRepository<MarriageScheduleEntity, UUID> {
    List<MarriageScheduleEntity> findByMarriageCaseId(UUID marriageCaseId);
    Optional<MarriageScheduleEntity> findFirstByMarriageCaseIdAndScheduleStatusOrderByApprovedDateTimeDesc(
            UUID marriageCaseId,
            MarriageScheduleStatus scheduleStatus
    );
}
