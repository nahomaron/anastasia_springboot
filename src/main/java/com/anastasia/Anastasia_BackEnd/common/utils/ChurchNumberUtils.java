package com.anastasia.Anastasia_BackEnd.common.utils;

import org.springframework.util.StringUtils;

public final class ChurchNumberUtils {

    private static final String DEFAULT_PREFIX = "CH";

    private ChurchNumberUtils() {
        // helper class
    }

    public static String derivePrefix(String churchName) {
        if (!StringUtils.hasText(churchName)) {
            return DEFAULT_PREFIX;
        }
        String normalized = churchName.trim();
        if (normalized.length() >= 3 && normalized.regionMatches(true, 0, "st.", 0, 3)) {
            normalized = normalized.substring(3).trim();
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < normalized.length() && builder.length() < 2; i++) {
            char ch = normalized.charAt(i);
            if (Character.isLetter(ch)) {
                builder.append(Character.toUpperCase(ch));
            }
        }

        while (builder.length() < 2) {
            builder.append(DEFAULT_PREFIX.charAt(builder.length()));
        }

        return builder.substring(0, 2);
    }
}
