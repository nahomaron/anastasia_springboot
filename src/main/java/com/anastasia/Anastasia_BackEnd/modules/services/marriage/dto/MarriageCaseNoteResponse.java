package com.anastasia.Anastasia_BackEnd.modules.services.marriage.dto;

import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriageNoteVisibility;

import java.time.Instant;
import java.util.UUID;

public record MarriageCaseNoteResponse(
        UUID id,
        UUID partyId,
        UUID authorUserId,
        String noteType,
        MarriageNoteVisibility visibility,
        String content,
        Instant createdAt
) {
}
