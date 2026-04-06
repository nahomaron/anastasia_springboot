package com.anastasia.Anastasia_BackEnd.modules.calendar.dto;

import com.anastasia.Anastasia_BackEnd.modules.calendar.model.CalendarCategory;
import com.anastasia.Anastasia_BackEnd.modules.calendar.model.CalendarEntryType;
import com.anastasia.Anastasia_BackEnd.modules.calendar.model.CalendarSystem;
import com.anastasia.Anastasia_BackEnd.modules.calendar.model.CalendarVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record CalendarEntryRequest(
        @NotNull CalendarEntryType type,
        @NotBlank String title,
        String description,
        @NotNull CalendarSystem calendarSystem,
        @NotNull Instant startAtUtc,
        Instant endAtUtc,
        @NotBlank String timezone,
        boolean allDay,
        @NotNull CalendarVisibility visibility,
        Set<CalendarCategory> categories,
        CalendarRecurrenceRequest recurrence,
        Set<UUID> audienceUserIds,
        Set<Long> audienceGroupIds
) {

    public CalendarEntryRequest {
        categories = copySet(categories);
        audienceUserIds = copySet(audienceUserIds);
        audienceGroupIds = copySet(audienceGroupIds);
    }

    private static <T> Set<T> copySet(Set<T> input) {
        return input == null ? Set.of() : Set.copyOf(input);
    }
}
