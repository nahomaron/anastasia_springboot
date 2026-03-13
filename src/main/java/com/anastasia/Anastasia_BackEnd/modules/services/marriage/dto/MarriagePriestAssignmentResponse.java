package com.anastasia.Anastasia_BackEnd.modules.services.marriage.dto;

import java.time.Instant;
import java.util.UUID;

public record MarriagePriestAssignmentResponse(
        UUID id,
        UUID priestUserId,
        String priestNameSnapshot,
        Instant assignedAt,
        UUID assignedByUserId,
        boolean active,
        String assignmentNote
) {
}
