package com.anastasia.Anastasia_BackEnd.common.i18n;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum SupportedLanguage {
    ENGLISH("en", "en-US"),
    TIGRINYA("ti", "ti-ER");

    private final String code;
    private final String localeTag;

    SupportedLanguage(String code, String localeTag) {
        this.code = code;
        this.localeTag = localeTag;
    }

    public String code() {
        return code;
    }

    public String localeTag() {
        return localeTag;
    }

    public Locale locale() {
        return Locale.forLanguageTag(localeTag);
    }

    public static Optional<SupportedLanguage> from(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(language ->
                        language.code.equals(normalized)
                                || language.localeTag.toLowerCase(Locale.ROOT).equals(normalized)
                                || normalized.startsWith(language.code + "-")
                                || normalized.startsWith(language.code + "_"))
                .findFirst();
    }
}
