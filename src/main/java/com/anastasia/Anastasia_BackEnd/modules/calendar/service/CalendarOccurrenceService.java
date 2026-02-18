package com.anastasia.Anastasia_BackEnd.modules.calendar.service;

import com.anastasia.Anastasia_BackEnd.modules.calendar.dto.CalendarOccurrenceResponse;
import com.anastasia.Anastasia_BackEnd.modules.calendar.model.CalendarEntryType;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public interface CalendarOccurrenceService {

    List<CalendarOccurrenceResponse> getOccurrences(
            Instant rangeStart,
            Instant rangeEnd,
            Set<CalendarEntryType> types,
            UUID userId,
            Set<String> authorities
    );
}
