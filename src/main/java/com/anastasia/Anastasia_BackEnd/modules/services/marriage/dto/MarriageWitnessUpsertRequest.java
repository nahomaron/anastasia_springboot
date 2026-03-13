package com.anastasia.Anastasia_BackEnd.modules.services.marriage.dto;

import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriagePartyRole;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriageWitnessType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record MarriageWitnessUpsertRequest(
        MarriagePartyRole partyRole,
        @NotNull MarriageWitnessType witnessType,
        @NotBlank String nameEnglish,
        String nameLocal,
        String relationshipToParty,
        String phone,
        String email,
        String addressLine,
        String idType,
        String idNumber,
        String idDocumentReference,
        boolean testimonyCompleted,
        LocalDate testimonyDate,
        String notes,
        Integer sortOrder
) {
}
