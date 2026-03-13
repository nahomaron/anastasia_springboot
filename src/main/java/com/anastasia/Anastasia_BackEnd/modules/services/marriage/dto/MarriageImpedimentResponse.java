package com.anastasia.Anastasia_BackEnd.modules.services.marriage.dto;

import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriageImpedimentSeverity;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriageImpedimentStatus;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriageImpedimentType;

import java.util.UUID;

public record MarriageImpedimentResponse(
        UUID id,
        UUID partyId,
        MarriageImpedimentType impedimentType,
        MarriageImpedimentSeverity severity,
        String sourceStage,
        boolean blocking,
        MarriageImpedimentStatus status,
        UUID createdByUserId,
        UUID resolvedByUserId,
        String evidenceNote
) {
}
