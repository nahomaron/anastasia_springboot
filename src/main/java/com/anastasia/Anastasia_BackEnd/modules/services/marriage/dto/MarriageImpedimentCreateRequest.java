package com.anastasia.Anastasia_BackEnd.modules.services.marriage.dto;

import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriageImpedimentSeverity;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriageImpedimentType;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriagePartyRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MarriageImpedimentCreateRequest(
        MarriagePartyRole partyRole,
        @NotNull MarriageImpedimentType impedimentType,
        @NotNull MarriageImpedimentSeverity severity,
        boolean blocking,
        @NotBlank String sourceStage,
        @NotBlank String evidenceNote
) {
}
