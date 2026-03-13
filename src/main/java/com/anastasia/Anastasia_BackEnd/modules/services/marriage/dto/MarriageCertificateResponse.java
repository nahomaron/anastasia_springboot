package com.anastasia.Anastasia_BackEnd.modules.services.marriage.dto;

import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriageCertificateStatus;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

public record MarriageCertificateResponse(
        UUID id,
        UUID caseId,
        String certificateNumber,
        String numberingFormatSnapshot,
        Instant issuedDate,
        UUID issuedByUserId,
        JsonNode lockedSnapshot,
        int printCount,
        String registryReference,
        MarriageCertificateStatus status,
        boolean hasAmendment
) {
}
