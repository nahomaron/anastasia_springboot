package com.anastasia.Anastasia_BackEnd.modules.calendar.service;

import com.anastasia.Anastasia_BackEnd.modules.calendar.dto.CalendarEntryRequest;
import com.anastasia.Anastasia_BackEnd.modules.calendar.dto.CalendarEntryResponse;
import com.anastasia.Anastasia_BackEnd.modules.calendar.dto.OccurrenceOverrideRequest;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public interface CalendarEntryService {

    CalendarEntryResponse createEntry(CalendarEntryRequest request, UUID ownerUserId);

    CalendarEntryResponse updateEntry(UUID entryId, CalendarEntryRequest request, UUID ownerUserId);

    void applyOccurrenceOverride(UUID entryId, OccurrenceOverrideRequest request, UUID ownerUserId);

    CalendarEntryResponse splitSeries(
            UUID entryId,
            java.time.LocalDate occurrenceDate,
            CalendarEntryRequest newSeriesRequest,
            UUID ownerUserId
    );
}
