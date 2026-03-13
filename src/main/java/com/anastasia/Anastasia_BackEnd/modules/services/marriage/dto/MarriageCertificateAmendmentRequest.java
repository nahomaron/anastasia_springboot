package com.anastasia.Anastasia_BackEnd.modules.services.marriage.dto;

import jakarta.validation.constraints.NotBlank;

public record MarriageCertificateAmendmentRequest(
        @NotBlank String amendmentReason,
        @NotBlank String amendmentSnapshotJson
) {
}
