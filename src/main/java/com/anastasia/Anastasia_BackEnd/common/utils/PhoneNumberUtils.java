package com.anastasia.Anastasia_BackEnd.common.utils;

public final class PhoneNumberUtils {

    private PhoneNumberUtils() {}

    public static String normalize(String phone) {
        if (phone == null) {
            return null;
        }

        String cleaned = phone.trim()
                .replace(" ", "")
                .replace("-", "")
                .replace("(", "")
                .replace(")", "");

        if (cleaned.startsWith("00")) {
            return "+" + cleaned.substring(2);
        }

        return cleaned;
    }

    public static String mask(String phone) {
        String normalized = normalize(phone);
        if (normalized == null || normalized.isBlank()) {
            return "unknown";
        }

        int length = normalized.length();
        if (length <= 6) {
            return "***";
        }

        String prefix = normalized.substring(0, 3);
        String suffix = normalized.substring(length - 2);
        return prefix + "***" + suffix;
    }
}
