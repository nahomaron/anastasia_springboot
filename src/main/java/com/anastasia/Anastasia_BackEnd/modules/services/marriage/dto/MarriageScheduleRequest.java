package com.anastasia.Anastasia_BackEnd.modules.services.marriage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record MarriageScheduleRequest(
        @NotNull Instant dateTime,
        @NotBlank String placeLabel,
        @NotBlank String timezone,
        String schedulingNote
) {
}
