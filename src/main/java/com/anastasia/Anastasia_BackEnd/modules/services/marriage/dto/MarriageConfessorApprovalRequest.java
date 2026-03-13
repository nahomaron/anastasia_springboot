package com.anastasia.Anastasia_BackEnd.modules.services.marriage.dto;

import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriagePartyRole;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record MarriageConfessorApprovalRequest(
        MarriagePartyRole partyRole,
        @NotNull UUID priestUserId,
        String notes,
        LocalDate approvalDate
) {
}
