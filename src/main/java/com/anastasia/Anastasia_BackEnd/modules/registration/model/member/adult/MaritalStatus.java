package com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult;

import java.util.Locale;

public enum MaritalStatus {
    SINGLE,
    MARRIED,
    ENGAGED,
    DIVORCED,
    WIDOWED,
    SEPARATED,
    RELIGIOUS,
    OTHER;

    public static MaritalStatus from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim().replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "SINGLE" -> SINGLE;
            case "MARRIED" -> MARRIED;
            case "ENGAGED" -> ENGAGED;
            case "DIVORCED" -> DIVORCED;
            case "WIDOWED" -> WIDOWED;
            case "SEPARATED" -> SEPARATED;
            case "RELIGIOUS" -> RELIGIOUS;
            default -> OTHER;
        };
    }
}
