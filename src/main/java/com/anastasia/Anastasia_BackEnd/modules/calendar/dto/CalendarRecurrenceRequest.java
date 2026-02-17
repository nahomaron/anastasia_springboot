package com.anastasia.Anastasia_BackEnd.modules.calendar.dto;

import com.anastasia.Anastasia_BackEnd.modules.calendar.model.CalendarSystem;
import com.anastasia.Anastasia_BackEnd.modules.calendar.model.RecurrenceFrequency;

import java.time.DayOfWeek;
import java.time.Instant;
import java.util.Set;

public record CalendarRecurrenceRequest(
        RecurrenceFrequency frequency,
        Integer interval,
        Set<DayOfWeek> byDay,
        Set<Integer> byMonth,
        Set<Integer> byMonthDay,
        Instant until,
        Integer count,
        CalendarSystem calendarSystem,
        Integer geezMonth,
        Integer geezDay
) {}
