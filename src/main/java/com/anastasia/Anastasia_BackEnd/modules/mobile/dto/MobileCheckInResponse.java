package com.anastasia.Anastasia_BackEnd.modules.mobile.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record MobileCheckInResponse(
        Long attendanceId,
        Long eventId,
        UUID memberId,
        String attendeeType,
        String attendeeName,
        String status,
        String checkInMethod,
        LocalDateTime checkInTime,
        boolean duplicate,
        String message
) {
}
