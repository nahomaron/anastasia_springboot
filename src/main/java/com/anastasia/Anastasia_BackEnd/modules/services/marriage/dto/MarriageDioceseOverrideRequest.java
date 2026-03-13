package com.anastasia.Anastasia_BackEnd.modules.services.marriage.dto;

import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriagePartyRole;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record MarriageDioceseOverrideRequest(
        MarriagePartyRole partyRole,
        UUID overrideDocumentId,
        @NotBlank String overrideReason,
        @NotBlank String notes
) {
}
