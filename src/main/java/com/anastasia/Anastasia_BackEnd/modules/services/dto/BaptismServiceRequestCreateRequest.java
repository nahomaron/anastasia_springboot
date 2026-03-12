package com.anastasia.Anastasia_BackEnd.modules.services.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record BaptismServiceRequestCreateRequest(
        @NotBlank(message = "churchNumber is required")
        String churchNumber,
        @NotNull(message = "birthDate is required")
        LocalDate birthDate,
        @NotNull(message = "baptismDate is required")
        LocalDate baptismDate,
        @Valid
        @NotNull(message = "localLanguage is required")
        BaptismLanguageDetailsRequest localLanguage,
        @Valid
        @NotNull(message = "english is required")
        BaptismLanguageDetailsRequest english,
        @Valid
        @NotNull(message = "babyPhoto is required")
        UploadedDocumentRequest babyPhoto,
        @Valid
        @NotNull(message = "birthCertificate is required")
        UploadedDocumentRequest birthCertificate,
        @Valid
        @NotNull(message = "fatherSignature is required")
        UploadedDocumentRequest fatherSignature,
        @Valid
        @NotNull(message = "priestSignature is required")
        UploadedDocumentRequest priestSignature
) {
}
