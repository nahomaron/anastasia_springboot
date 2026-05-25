package com.anastasia.Anastasia_BackEnd.modules.events.model.attendance;

import com.anastasia.Anastasia_BackEnd.modules.events.model.EventType;
import lombok.Builder;

import java.time.Instant;
import java.time.LocalDateTime;

@Builder
public record MemberAttendanceReportResponse(
        Long id,
        Long eventId,
        String eventTitle,
        EventType eventType,
        Instant eventStartAt,
        String eventTimezone,
        String location,
        LocalDateTime checkInTime,
        String checkInMethod,
        AttendanceStatus status
) {
}
