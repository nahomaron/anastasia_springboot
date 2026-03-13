package com.anastasia.Anastasia_BackEnd.modules.services.marriage.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record MarriagePriestAssignmentRequest(
        @NotNull UUID priestUserId,
        String assignmentNote
) {
}
