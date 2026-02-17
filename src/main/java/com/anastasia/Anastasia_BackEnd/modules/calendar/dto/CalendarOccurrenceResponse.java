package com.anastasia.Anastasia_BackEnd.modules.calendar.dto;

import com.anastasia.Anastasia_BackEnd.modules.calendar.model.CalendarCategory;
import com.anastasia.Anastasia_BackEnd.modules.calendar.model.CalendarEntryType;
import com.anastasia.Anastasia_BackEnd.modules.calendar.model.CalendarSystem;
import com.anastasia.Anastasia_BackEnd.modules.calendar.model.CalendarVisibility;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public record CalendarOccurrenceResponse(
        UUID entryId,
        CalendarEntryType type,
        String title,
        String description,
        CalendarSystem calendarSystem,
        Instant startAtUtc,
        Instant endAtUtc,
        String timezone,
        boolean allDay,
        CalendarVisibility visibility,
        LocalDate occurrenceDate,
        boolean cancelled,
        Set<CalendarCategory> categories,
        UUID ownerUserId
) {}
