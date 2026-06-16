package com.anastasia.Anastasia_BackEnd.common.security;

import com.anastasia.Anastasia_BackEnd.common.config.PublicUrlUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@Slf4j
public class OAuth2AuthenticationFailureHandler implements AuthenticationFailureHandler {

    @Value("${app.public.frontend-base-url:}")
    private String frontendBaseUrl;

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {
        String resolvedError = resolveErrorMessage(request, exception);
        log.warn(
                "Google OAuth authentication failed: requestError={}, requestErrorDescription={}, resolvedError={}",
                trimToNull(request.getParameter("error")),
                trimToNull(request.getParameter("error_description")),
                resolvedError,
                exception
        );

        String redirectUrl = UriComponentsBuilder
                .fromUriString(normalizeBaseUrl(frontendBaseUrl) + "/auth/google/callback")
                .queryParam("error", resolvedError)
                .build()
                .encode()
                .toUriString();

        response.sendRedirect(redirectUrl);
    }

    private String resolveErrorMessage(HttpServletRequest request, AuthenticationException exception) {
        String providerDescription = trimToNull(request.getParameter("error_description"));
        if (providerDescription != null) {
            return providerDescription;
        }

        String exceptionMessage = trimToNull(exception == null ? null : exception.getMessage());
        if (exceptionMessage != null) {
            return exceptionMessage;
        }

        if (exception instanceof OAuth2AuthenticationException oauth2Exception) {
            String oauthDescription = trimToNull(oauth2Exception.getError().getDescription());
            if (oauthDescription != null) {
                return oauthDescription;
            }
        }

        Throwable rootCause = findRootCause(exception);
        String rootMessage = trimToNull(rootCause == null ? null : rootCause.getMessage());
        if (rootMessage != null) {
            return rootMessage;
        }

        String providerError = trimToNull(request.getParameter("error"));
        if (providerError != null) {
            return "OAuth provider returned error: " + providerError;
        }

        if (exception != null) {
            return "Google sign-in failed (" + exception.getClass().getSimpleName() + ").";
        }

        return "Google sign-in failed.";
    }

    private Throwable findRootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current != null && current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed;
    }

    private String normalizeBaseUrl(String value) {
        return PublicUrlUtils.normalizeBaseUrl(value, "app.public.frontend-base-url");
    }
}
