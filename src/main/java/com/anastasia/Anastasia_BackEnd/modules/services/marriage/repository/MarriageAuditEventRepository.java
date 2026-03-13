package com.anastasia.Anastasia_BackEnd.modules.services.marriage.repository;

import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriageAuditEventEntity;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriageCaseAuditEventType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MarriageAuditEventRepository extends JpaRepository<MarriageAuditEventEntity, UUID> {
    List<MarriageAuditEventEntity> findByMarriageCaseIdOrderByOccurredAtDesc(UUID marriageCaseId);
    List<MarriageAuditEventEntity> findByMarriageCaseIdAndEventTypeOrderByOccurredAtDesc(
            UUID marriageCaseId,
            MarriageCaseAuditEventType eventType
    );
}
