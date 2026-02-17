package com.anastasia.Anastasia_BackEnd.modules.calendar.service;

import com.anastasia.Anastasia_BackEnd.modules.calendar.dto.CalendarOccurrenceResponse;
import com.anastasia.Anastasia_BackEnd.modules.calendar.model.CalendarEntryType;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface CalendarOccurrenceService {

    List<CalendarOccurrenceResponse> getOccurrences(
            Instant rangeStart,
            Instant rangeEnd,
            Set<CalendarEntryType> types,
            UUID userId,
            Set<String> authorities
    );
}
