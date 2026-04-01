package com.anastasia.Anastasia_BackEnd.core.notification.dto;

import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationType;

import java.util.Set;

public record NotificationPreferencesResponse(
        boolean emailEnabled,
        boolean smsEnabled,
        boolean inAppEnabled,
        Set<NotificationType> mutedTypes
) {

    public NotificationPreferencesResponse(
            boolean emailEnabled,
            boolean smsEnabled,
            boolean inAppEnabled,
            Set<NotificationType> mutedTypes
    ) {
        this.emailEnabled = emailEnabled;
        this.smsEnabled = smsEnabled;
        this.inAppEnabled = inAppEnabled;
        this.mutedTypes = copySet(mutedTypes);
    }

    private static <T> Set<T> copySet(Set<T> input) {
        return input == null ? Set.of() : Set.copyOf(input);
    }
}
