package com.anastasia.Anastasia_BackEnd.common.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PublicUrlStartupValidation {

    private final Environment environment;

    @Value("${app.public.frontend-base-url:}")
    private String frontendBaseUrl;

    @Value("${app.public.backend-base-url:}")
    private String backendBaseUrl;

    @Value("${app.cors.allowed-origins:}")
    private String allowedOrigins;

    @PostConstruct
    public void validate() {
        if (PublicUrlUtils.isLocalProfile(environment)) {
            return;
        }

        List<String> violations = new ArrayList<>();
        validateHttpsUrl(frontendBaseUrl, "app.public.frontend-base-url", violations);
        validateHttpsUrl(backendBaseUrl, "app.public.backend-base-url", violations);
        validateHttpsOrigins(allowedOrigins, violations);

        if (!violations.isEmpty()) {
            throw new IllegalStateException("HTTPS-only public URL validation failed: " + String.join(", ", violations));
        }
    }

    private void validateHttpsUrl(String rawUrl, String propertyName, List<String> violations) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return;
        }
        if (!PublicUrlUtils.isHttpsUrl(rawUrl)) {
            violations.add(propertyName + " must use https");
        }
    }

    private void validateHttpsOrigins(String rawOrigins, List<String> violations) {
        if (rawOrigins == null || rawOrigins.isBlank()) {
            return;
        }

        for (String origin : rawOrigins.split(",")) {
            String trimmed = origin.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (!PublicUrlUtils.isHttpsUrl(trimmed)) {
                violations.add("app.cors.allowed-origins contains non-https origin: " + trimmed);
            }
        }
    }
}
