package com.anastasia.Anastasia_BackEnd.modules.registration.model.member;

import java.util.Locale;

public enum EducationLevel {
    NONE,
    PRIMARY,
    SECONDARY,
    DIPLOMA,
    BACHELORS,
    MASTERS,
    DOCTORATE,
    VOCATIONAL,
    OTHER,
    UNSPECIFIED;

    public static EducationLevel from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim().replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "NONE", "NO_FORMAL_EDUCATION" -> NONE;
            case "PRIMARY", "ELEMENTARY" -> PRIMARY;
            case "SECONDARY", "HIGH_SCHOOL" -> SECONDARY;
            case "DIPLOMA", "ASSOCIATE", "ASSOCIATES" -> DIPLOMA;
            case "BACHELOR", "BACHELORS" -> BACHELORS;
            case "MASTER", "MASTERS" -> MASTERS;
            case "DOCTORATE", "PHD", "PH_D" -> DOCTORATE;
            case "VOCATIONAL", "TRADE_SCHOOL" -> VOCATIONAL;
            case "UNSPECIFIED", "UNKNOWN", "NA", "N_A" -> UNSPECIFIED;
            default -> OTHER;
        };
    }
}
