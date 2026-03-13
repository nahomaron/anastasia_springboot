package com.anastasia.Anastasia_BackEnd.modules.services.marriage.dto;

import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriagePartyRole;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriagePartySubmissionStatus;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

public record MarriagePartyApplicationResponse(
        UUID partyId,
        MarriagePartyRole partyRole,
        boolean submitted,
        boolean editable,
        MarriagePartySubmissionStatus latestSubmissionStatus,
        Integer latestSubmissionVersion,
        Instant submittedAt,
        JsonNode latestSnapshot
) {
}
