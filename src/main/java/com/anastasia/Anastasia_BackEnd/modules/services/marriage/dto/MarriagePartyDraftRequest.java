package com.anastasia.Anastasia_BackEnd.modules.services.marriage.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record MarriagePartyDraftRequest(
        @NotBlank String fullLegalNameEnglish,
        String fullLegalNameLocal,
        String baptismalName,
        @NotNull LocalDate dateOfBirth,
        @NotBlank String maritalStatus,
        String profession,
        @Email String email,
        String phone,
        String alternatePhone,
        String addressLine,
        String currentCountry,
        String currentCity,
        String nationalityEnglish,
        String nationalityLocal,
        String placeOfBirthEnglish,
        String placeOfBirthLocal,
        String placeOfOriginEnglish,
        String placeOfOriginLocal,
        String governmentIdType,
        String governmentIdNumber,
        String passportNumber,
        String baptismChurchEnglish,
        String baptismChurchLocal,
        String currentChurchEnglish,
        String currentChurchLocal,
        String dioceseEnglish,
        String dioceseLocal,
        String fatherOfConfessionName,
        String fatherOfConfessionChurch,
        String confessorSelectionMode,
        Integer previousMarriagesCount,
        Integer numberOfChildren,
        String legalCivilEvidenceSummary,
        String priorMaritalHistoryNotes
) {
}
