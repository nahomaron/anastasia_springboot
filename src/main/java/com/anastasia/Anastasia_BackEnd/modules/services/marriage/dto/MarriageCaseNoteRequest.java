package com.anastasia.Anastasia_BackEnd.modules.services.marriage.dto;

import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriageNoteVisibility;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriagePartyRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MarriageCaseNoteRequest(
        MarriagePartyRole partyRole,
        @NotBlank String noteType,
        @NotNull MarriageNoteVisibility visibility,
        @NotBlank String content
) {
}
