package com.anastasia.Anastasia_BackEnd.common.config;

import org.springframework.core.env.Environment;

import java.net.URI;
import java.util.Arrays;
import java.util.Locale;

public final class PublicUrlUtils {

    private PublicUrlUtils() {
    }

    public static boolean isLocalProfile(Environment environment) {
        return Arrays.stream(environment.getActiveProfiles())
                .map(profile -> profile.toLowerCase(Locale.ROOT))
                .anyMatch(profile ->
                        "dev".equals(profile)
                                || "test".equals(profile)
                                || "api-tests".equals(profile)
                                || "local".equals(profile));
    }

    public static String normalizeBaseUrl(String rawUrl, String propertyName) {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw new IllegalStateException(propertyName + " must be configured");
        }

        String normalized = rawUrl.trim();
        URI uri = parseUri(normalized, propertyName);
        String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            throw new IllegalStateException(propertyName + " must use http or https");
        }
        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new IllegalStateException(propertyName + " must be an absolute URL");
        }

        return normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
    }

    public static boolean isHttpsUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return false;
        }
        URI uri = parseUri(rawUrl.trim(), "URL");
        return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null && !uri.getHost().isBlank();
    }

    private static URI parseUri(String rawUrl, String propertyName) {
        try {
            return URI.create(rawUrl);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(propertyName + " must be a valid absolute URL", ex);
        }
    }
}
