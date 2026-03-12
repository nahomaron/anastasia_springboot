package com.anastasia.Anastasia_BackEnd.modules.registration.model.member;

import java.util.Locale;

public enum MemberGender {
    MALE,
    FEMALE,
    OTHER,
    UNSPECIFIED;

    public static MemberGender from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim().replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "M", "MALE" -> MALE;
            case "F", "FEMALE" -> FEMALE;
            case "OTHER" -> OTHER;
            case "UNSPECIFIED", "UNKNOWN", "NA", "N_A" -> UNSPECIFIED;
            default -> OTHER;
        };
    }
}
