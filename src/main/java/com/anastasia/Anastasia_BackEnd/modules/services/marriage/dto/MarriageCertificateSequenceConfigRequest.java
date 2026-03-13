package com.anastasia.Anastasia_BackEnd.modules.services.marriage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MarriageCertificateSequenceConfigRequest(
        @NotBlank String churchNumber,
        String prefix,
        String separator,
        @NotNull Long startingSeed,
        String resetMode,
        @NotBlank String formatMask,
        String migrationReference
) {
}
