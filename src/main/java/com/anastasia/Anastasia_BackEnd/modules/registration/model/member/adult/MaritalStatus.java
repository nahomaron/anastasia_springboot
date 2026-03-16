package com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

public enum MaritalStatus {
    SINGLE("Single"),
    MARRIED("Married"),
    ENGAGED("Engaged"),
    DIVORCED("Divorced"),
    WIDOWED("Widowed"),
    SEPARATED("Separated"),
    RELIGIOUS("Religious"),
    OTHER("Other");

    private final String apiValue;

    MaritalStatus(String apiValue) {
        this.apiValue = apiValue;
    }

    @JsonCreator
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

    @JsonValue
    public String toApiValue() {
        return apiValue;
    }
}
