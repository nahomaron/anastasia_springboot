package com.anastasia.Anastasia_BackEnd.modules.events.model.attendance;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record EventAttendanceResponse(
        Long id,
        Long eventId,
        UUID userId,
        LocalDateTime checkInTime,
        String checkInMethod,
        AttendanceStatus status,
        UUID checkedInBy
) {
}
