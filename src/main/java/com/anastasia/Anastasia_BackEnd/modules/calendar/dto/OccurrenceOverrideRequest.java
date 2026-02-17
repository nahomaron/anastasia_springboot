package com.anastasia.Anastasia_BackEnd.modules.calendar.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.time.LocalDate;

public record OccurrenceOverrideRequest(
        @NotNull LocalDate occurrenceDate,
        boolean cancelled,
        String titleOverride,
        Instant startAtUtcOverride,
        Instant endAtUtcOverride,
        String notes
) {}
