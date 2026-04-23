package com.anastasia.Anastasia_BackEnd.common.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
public class OAuth2AuthenticationFailureHandler implements AuthenticationFailureHandler {

    @Value("${app.public.frontend-base-url:}")
    private String frontendBaseUrl;

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {
        String redirectUrl = UriComponentsBuilder
                .fromUriString(normalizeBaseUrl(frontendBaseUrl) + "/auth/google/callback")
                .queryParam("error", exception == null ? "Google sign-in failed." : exception.getMessage())
                .build(true)
                .toUriString();

        response.sendRedirect(redirectUrl);
    }

    private String normalizeBaseUrl(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("app.public.frontend-base-url must be configured");
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
