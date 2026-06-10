package com.anastasia.Anastasia_BackEnd.modules.services.marriage.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record MarriageDocumentResponse(
        UUID id,
        UUID partyId,
        String documentCategory,
        String originalFileName,
        String storageReference,
        String contentType,
        String verificationStatus,
        LocalDate expiryDate,
        String notes,
        UUID uploadedByUserId,
        Instant uploadedAt
) {
}
