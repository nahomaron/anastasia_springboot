package com.anastasia.Anastasia_BackEnd.modules.registration.service.onboarding;

import com.anastasia.Anastasia_BackEnd.common.i18n.LocalizedMessageService;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantOnboardingSessionEntity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;

@Service
public class OnboardingSessionAccessService {

    public static final String ONBOARDING_ACCESS_COOKIE_NAME = "anastasia_onboarding_access";
    public static final String ONBOARDING_ACCESS_HEADER_NAME = "X-Onboarding-Token";
    private static final String SAME_SITE_NONE = "None";
    private static final String SAME_SITE_LAX = "Lax";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final LocalizedMessageService messageService;

    @Value("${app.onboarding.access-cookie.secure:true}")
    private boolean secureCookie;

    @Value("${app.onboarding.access-cookie.same-site:None}")
    private String sameSite;

    @Value("${app.onboarding.access-cookie.domain:}")
    private String cookieDomain;

    public OnboardingSessionAccessService(LocalizedMessageService messageService) {
        this.messageService = messageService;
    }

    public String issueAccessToken(TenantOnboardingSessionEntity session) {
        String rawToken = generateToken();
        session.setAccessTokenHash(hashToken(rawToken));
        return rawToken;
    }

    public void assertSessionAccess(TenantOnboardingSessionEntity session, String presentedToken) {
        if (presentedToken == null || presentedToken.isBlank()) {
            throw invalidAccessToken();
        }
        if (session.getExpiresAt() != null && session.getExpiresAt().isBefore(Instant.now())) {
            throw invalidAccessToken();
        }
        String expectedHash = session.getAccessTokenHash();
        if (expectedHash == null || expectedHash.isBlank()) {
            throw invalidAccessToken();
        }
        if (!MessageDigest.isEqual(
                expectedHash.getBytes(StandardCharsets.UTF_8),
                hashToken(presentedToken).getBytes(StandardCharsets.UTF_8)
        )) {
            throw invalidAccessToken();
        }
    }

    public void addAccessTokenCookie(HttpServletResponse response, String accessToken, Instant expiresAt) {
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(accessToken, maxAge(expiresAt)).toString());
    }

    public void clearAccessTokenCookie(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie("", Duration.ZERO).toString());
    }

    public Optional<String> extractAccessToken(HttpServletRequest request) {
        String headerValue = request.getHeader(ONBOARDING_ACCESS_HEADER_NAME);
        if (headerValue != null && !headerValue.isBlank()) {
            return Optional.of(headerValue.trim());
        }
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        return Arrays.stream(request.getCookies())
                .filter(cookie -> ONBOARDING_ACCESS_COOKIE_NAME.equals(cookie.getName()))
                .map(jakarta.servlet.http.Cookie::getValue)
                .filter(value -> value != null && !value.isBlank())
                .findFirst();
    }

    private Duration maxAge(Instant expiresAt) {
        if (expiresAt == null) {
            return Duration.ofHours(24);
        }
        Duration remaining = Duration.between(Instant.now(), expiresAt);
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }

    private ResponseCookie buildCookie(String value, Duration maxAge) {
        String resolvedSameSite = resolveSameSite();
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(ONBOARDING_ACCESS_COOKIE_NAME, value)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite(resolvedSameSite)
                .path("/api/v1/onboarding/billing")
                .maxAge(maxAge);

        if (cookieDomain != null && !cookieDomain.isBlank()) {
            builder.domain(cookieDomain);
        }

        return builder.build();
    }

    private String resolveSameSite() {
        if (!secureCookie && SAME_SITE_NONE.equalsIgnoreCase(sameSite)) {
            return SAME_SITE_LAX;
        }
        return sameSite;
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    private AccessDeniedException invalidAccessToken() {
        return new AccessDeniedException(messageService.get(
                "onboarding.session.access.denied",
                "Onboarding session access token is missing or invalid."
        ));
    }
}
