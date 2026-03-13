package com.anastasia.Anastasia_BackEnd.modules.services.marriage.dto;

import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriageLanguageCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MarriageAdminInitiationRequest(
        @NotBlank String churchNumber,
        MarriageLanguageCode primaryLanguage,
        @NotNull @Valid MarriagePartyCreateRequest bride,
        @NotNull @Valid MarriagePartyCreateRequest groom
) {
}
