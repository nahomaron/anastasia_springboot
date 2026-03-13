package com.anastasia.Anastasia_BackEnd.modules.services.marriage.dto;

import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriageWitnessType;

import java.time.LocalDate;
import java.util.UUID;

public record MarriageWitnessResponse(
        UUID id,
        UUID partyId,
        MarriageWitnessType witnessType,
        String nameEnglish,
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
        UUID verifiedByUserId,
        String notes,
        Integer sortOrder
) {
}
