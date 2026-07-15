package com.anastasia.Anastasia_BackEnd.modules.mobile.dto;

import java.time.Instant;

public record MobileNotificationItem(
        Long id,
        String title,
        String message,
        String type,
        String scope,
        boolean read,
        Instant createdAt,
        Instant readAt
) {
}
