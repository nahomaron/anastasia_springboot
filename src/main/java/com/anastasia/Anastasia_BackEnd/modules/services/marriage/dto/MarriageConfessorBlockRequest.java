package com.anastasia.Anastasia_BackEnd.modules.services.marriage.dto;

import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriagePartyRole;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record MarriageConfessorBlockRequest(
        MarriagePartyRole partyRole,
        UUID priestUserId,
        @NotBlank String notes
) {
}
