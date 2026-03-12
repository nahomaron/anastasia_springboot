package com.anastasia.Anastasia_BackEnd.modules.calendar.dto;

import com.anastasia.Anastasia_BackEnd.modules.calendar.model.CalendarCategory;
import com.anastasia.Anastasia_BackEnd.modules.calendar.model.CalendarEntrySourceType;
import com.anastasia.Anastasia_BackEnd.modules.calendar.model.CalendarEntryStatus;
import com.anastasia.Anastasia_BackEnd.modules.calendar.model.CalendarEntryType;
import com.anastasia.Anastasia_BackEnd.modules.calendar.model.CalendarSystem;
import com.anastasia.Anastasia_BackEnd.modules.calendar.model.CalendarVisibility;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record CalendarEntryResponse(
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
        CalendarEntryStatus status,
        CalendarEntrySourceType sourceEntityType,
        UUID sourceEntityId,
        Set<CalendarCategory> categories,
        UUID ownerUserId
) {}
