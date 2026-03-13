package com.anastasia.Anastasia_BackEnd.modules.services.marriage.dto;

import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriageScheduleStatus;

import java.time.Instant;
import java.util.UUID;

public record MarriageScheduleResponse(
        UUID id,
        Instant proposedDateTime,
        Instant approvedDateTime,
        String placeLabel,
        UUID adminCalendarEventId,
        UUID priestCalendarEventId,
        MarriageScheduleStatus scheduleStatus,
        int rescheduleCount,
        UUID assignedPriestUserId,
        String schedulingNote
) {
}
