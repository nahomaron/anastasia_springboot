package com.anastasia.Anastasia_BackEnd.modules.services.marriage.dto;

import java.util.UUID;

public record MarriagePriestLookupResponse(
        UUID userId,
        String fullName,
        String email
) {
}
