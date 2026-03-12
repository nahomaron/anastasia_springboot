package com.anastasia.Anastasia_BackEnd.modules.users.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserPreferencesRequest {
    private String themeMode;
    private String language;
    private String locale;
    private String dateFormat;
    private String firstDayOfWeek;
    private Boolean reducedMotion;
    private Boolean compactUi;
    private Boolean emailNotifications;
    private Boolean pushNotifications;
    private Boolean marketingNotifications;
    private Boolean sharePresence;
    private Boolean analyticsOptIn;
    private Boolean autoDetectLocation;
}
