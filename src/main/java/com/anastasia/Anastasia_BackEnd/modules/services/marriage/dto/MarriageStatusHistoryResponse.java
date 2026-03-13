package com.anastasia.Anastasia_BackEnd.modules.services.marriage.dto;

import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriageCaseStatus;

import java.time.Instant;
import java.util.UUID;

public record MarriageStatusHistoryResponse(
        UUID id,
        MarriageCaseStatus fromStatus,
        MarriageCaseStatus toStatus,
        String changeReason,
        UUID changedByUserId,
        Instant changedAt
) {
}
