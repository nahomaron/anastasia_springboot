package com.anastasia.Anastasia_BackEnd.modules.services.marriage.dto;

import java.time.Instant;
import java.util.UUID;

public record MarriageCertificateAmendmentResponse(
        UUID id,
        UUID certificateId,
        String amendmentReason,
        String amendmentSnapshotJson,
        UUID amendedByUserId,
        Instant amendedAt
) {
}
