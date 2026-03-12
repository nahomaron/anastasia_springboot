package com.anastasia.Anastasia_BackEnd.modules.services.dto;

import jakarta.validation.constraints.NotBlank;

public record BaptismLanguageDetailsRequest(
        @NotBlank(message = "fullName is required")
        String fullName,
        @NotBlank(message = "baptismalName is required")
        String baptismalName,
        @NotBlank(message = "fatherFullName is required")
        String fatherFullName,
        @NotBlank(message = "motherFullName is required")
        String motherFullName,
        @NotBlank(message = "godParentFullName is required")
        String godParentFullName,
        @NotBlank(message = "priestFullName is required")
        String priestFullName,
        @NotBlank(message = "churchOfBaptismName is required")
        String churchOfBaptismName
) {
}
