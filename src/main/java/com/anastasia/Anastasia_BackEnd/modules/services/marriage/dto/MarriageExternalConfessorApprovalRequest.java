package com.anastasia.Anastasia_BackEnd.modules.services.marriage.dto;

import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriagePartyRole;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.util.UUID;

public record MarriageExternalConfessorApprovalRequest(
        MarriagePartyRole partyRole,
        @NotBlank String priestPersonName,
        String churchName,
        String dioceseName,
        LocalDate approvalDate,
        UUID evidenceDocumentId,
        @NotBlank String notes
) {
}
