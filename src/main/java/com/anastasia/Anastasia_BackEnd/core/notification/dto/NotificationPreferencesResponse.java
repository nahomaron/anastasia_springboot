package com.anastasia.Anastasia_BackEnd.core.notification.dto;

import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationType;

import java.util.Set;

public record NotificationPreferencesResponse(
        boolean emailEnabled,
        boolean smsEnabled,
        boolean inAppEnabled,
        Set<NotificationType> mutedTypes
) {
}
