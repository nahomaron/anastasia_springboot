package com.anastasia.Anastasia_BackEnd.modules.users.dto;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class UserPreferencesResponse {
    UUID userId;
    String themeMode;
    String language;
    String locale;
    String dateFormat;
    String firstDayOfWeek;
    boolean reducedMotion;
    boolean compactUi;
    boolean emailNotifications;
    boolean pushNotifications;
    boolean marketingNotifications;
    boolean sharePresence;
    boolean analyticsOptIn;
    boolean autoDetectLocation;
}
