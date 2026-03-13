package com.anastasia.Anastasia_BackEnd.modules.services.marriage.dto;

import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriagePartyRole;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriagePartySubmissionStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record MarriagePartySummaryResponse(
        UUID id,
        MarriagePartyRole partyRole,
        boolean counterpartPlaceholder,
        boolean externalApplicant,
        boolean submitted,
        MarriagePartySubmissionStatus latestSubmissionStatus,
        UUID linkedUserId,
        Long memberId,
        String fullLegalNameEnglish,
        String fullLegalNameLocal,
        LocalDate dateOfBirth,
        String maritalStatus,
        Instant submittedAt
) {
}
