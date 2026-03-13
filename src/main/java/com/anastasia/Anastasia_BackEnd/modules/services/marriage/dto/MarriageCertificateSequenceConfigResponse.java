package com.anastasia.Anastasia_BackEnd.modules.services.marriage.dto;

import java.util.UUID;

public record MarriageCertificateSequenceConfigResponse(
        UUID id,
        String churchNumber,
        String prefix,
        String separator,
        long currentNumber,
        long startingSeed,
        String resetMode,
        String formatMask,
        String migrationReference,
        boolean active
) {
}
