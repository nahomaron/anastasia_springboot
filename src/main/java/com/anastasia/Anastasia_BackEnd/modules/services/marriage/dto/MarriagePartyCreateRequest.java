package com.anastasia.Anastasia_BackEnd.modules.services.marriage.dto;

import jakarta.validation.constraints.Email;

public record MarriagePartyCreateRequest(
        String fullLegalNameEnglish,
        String fullLegalNameLocal,
        @Email String email,
        String phone,
        Boolean externalApplicant,
        Boolean placeholder
) {
}
