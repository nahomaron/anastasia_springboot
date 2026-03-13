package com.anastasia.Anastasia_BackEnd.modules.services.marriage.dto;

import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriageNoteVisibility;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriageReviewDecision;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriageReviewStage;

import java.time.Instant;
import java.util.UUID;

public record MarriageReviewResponse(
        UUID id,
        MarriageReviewStage stage,
        MarriageReviewDecision decision,
        UUID actorUserId,
        String actorRole,
        String reason,
        String notes,
        MarriageNoteVisibility visibility,
        Instant reviewedAt
) {
}
