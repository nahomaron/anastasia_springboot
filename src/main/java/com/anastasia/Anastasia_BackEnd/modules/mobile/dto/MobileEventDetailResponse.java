package com.anastasia.Anastasia_BackEnd.modules.mobile.dto;

import java.time.Instant;
import java.util.Map;

public record MobileEventDetailResponse(
        Long id,
        String title,
        String description,
        String status,
        String type,
        String visibility,
        Instant startAt,
        Instant endAt,
        String timezone,
        String location,
        boolean allDay,
        String imageUrl,
        Integer capacity,
        Instant checkInOpensAt,
        Instant checkInClosesAt,
        Boolean requiresRegistration,
        Boolean allowGeoCheckIn,
        Map<String, Object> attendeeSummary,
        Map<String, Boolean> actions
) {
}
