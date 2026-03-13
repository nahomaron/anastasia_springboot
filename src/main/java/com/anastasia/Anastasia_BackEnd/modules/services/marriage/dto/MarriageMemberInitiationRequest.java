package com.anastasia.Anastasia_BackEnd.modules.services.marriage.dto;

import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriageLanguageCode;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriagePartyRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MarriageMemberInitiationRequest(
        @NotBlank String churchNumber,
        @NotNull MarriagePartyRole initiatorPartyRole,
        MarriageLanguageCode primaryLanguage,
        String counterpartFullLegalNameEnglish,
        String counterpartFullLegalNameLocal,
        String counterpartEmail,
        String counterpartPhone,
        Boolean counterpartExternalApplicant
) {
}
