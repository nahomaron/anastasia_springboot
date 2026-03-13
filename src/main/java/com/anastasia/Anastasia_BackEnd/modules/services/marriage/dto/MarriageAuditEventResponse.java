package com.anastasia.Anastasia_BackEnd.modules.services.marriage.dto;

import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriageCaseAuditEventType;

import java.time.Instant;
import java.util.UUID;

public record MarriageAuditEventResponse(
        UUID id,
        MarriageCaseAuditEventType eventType,
        UUID actorUserId,
        UUID relatedPartyId,
        String summary,
        Instant occurredAt
) {
}
