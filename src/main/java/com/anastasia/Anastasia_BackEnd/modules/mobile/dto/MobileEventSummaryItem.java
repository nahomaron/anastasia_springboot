package com.anastasia.Anastasia_BackEnd.modules.mobile.dto;

import java.time.Instant;

public record MobileEventSummaryItem(
        Long id,
        String title,
        String description,
        String status,
        String type,
        Instant startAt,
        Instant endAt,
        String timezone,
        String location,
        boolean allDay,
        String imageUrl,
        boolean canCheckIn
) {
}
