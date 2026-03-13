package com.anastasia.Anastasia_BackEnd.modules.services.marriage.dto;

import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriagePartyRole;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record MarriageDocumentMetadataRequest(
        MarriagePartyRole partyRole,
        @NotBlank String documentCategory,
        @NotBlank String originalFileName,
        @NotBlank String storageReference,
        String contentType,
        LocalDate expiryDate,
        String documentNumber,
        String notes
) {
}
