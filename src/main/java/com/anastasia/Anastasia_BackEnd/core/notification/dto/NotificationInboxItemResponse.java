package com.anastasia.Anastasia_BackEnd.core.notification.dto;

import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationType;

import java.time.Instant;

public record NotificationInboxItemResponse(
        Long id,
        String title,
        String message,
        NotificationType type,
        boolean read,
        Instant createdAt,
        Instant readAt
) {
}
