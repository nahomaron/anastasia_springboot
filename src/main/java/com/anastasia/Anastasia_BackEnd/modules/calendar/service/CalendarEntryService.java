package com.anastasia.Anastasia_BackEnd.modules.calendar.service;

import com.anastasia.Anastasia_BackEnd.modules.calendar.dto.CalendarEntryRequest;
import com.anastasia.Anastasia_BackEnd.modules.calendar.dto.OccurrenceOverrideRequest;
import com.anastasia.Anastasia_BackEnd.modules.calendar.model.CalendarEntryEntity;

import java.util.UUID;

public interface CalendarEntryService {

    CalendarEntryEntity createEntry(CalendarEntryRequest request, UUID ownerUserId);

    CalendarEntryEntity updateEntry(UUID entryId, CalendarEntryRequest request, UUID ownerUserId);

    void applyOccurrenceOverride(UUID entryId, OccurrenceOverrideRequest request, UUID ownerUserId);

    CalendarEntryEntity splitSeries(
            UUID entryId,
            java.time.LocalDate occurrenceDate,
            CalendarEntryRequest newSeriesRequest,
            UUID ownerUserId
    );
}
