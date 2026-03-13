package com.anastasia.Anastasia_BackEnd.modules.services.marriage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record MarriageCeremonyCompletionRequest(
        @NotNull Instant ceremonyCompletedAt,
        @NotBlank String officiatingPriestName,
        String registryReference,
        String ceremonyNote
) {
}
