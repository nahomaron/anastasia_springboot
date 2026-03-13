package com.anastasia.Anastasia_BackEnd.modules.services.marriage.dto;

import jakarta.validation.constraints.NotBlank;

public record MarriageImpedimentResolveRequest(
        @NotBlank String evidenceNote,
        boolean waive
) {
}
