package com.anastasia.Anastasia_BackEnd.core.notification.dto;

import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationType;

import java.time.LocalDateTime;

public record NotificationRealtimeMessage(
        Long id,
        String title,
        String message,
        NotificationType type,
        LocalDateTime createdAt,
        boolean read
) {
}
