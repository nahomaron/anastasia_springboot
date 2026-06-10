package com.anastasia.Anastasia_BackEnd.common.security;

import com.anastasia.Anastasia_BackEnd.common.config.PublicUrlUtils;
import com.anastasia.Anastasia_BackEnd.core.auth.dto.AuthenticationResponse;
import com.anastasia.Anastasia_BackEnd.core.auth.service.AuthService;
import com.anastasia.Anastasia_BackEnd.core.auth.service.OAuthLoginTicketService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@Slf4j
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Value("${app.public.frontend-base-url:}")
    private String frontendBaseUrl;

    private final AuthService authService;
    private final OAuthLoginTicketService ticketService;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();

        String googleId = oauthUser.getAttribute("sub");
        String email = oauthUser.getAttribute("email");
        String fullName = oauthUser.getAttribute("name");
        Boolean emailVerified = oauthUser.getAttribute("email_verified");

        if (emailVerified != null && !emailVerified) {
            redirectWithError(response, "Google account email is not verified.");
            return;
        }

        try {
            AuthenticationResponse authResponse = authService.authenticateGoogleUser(googleId, email, fullName);
            String ticket = ticketService.store(authResponse);

            String redirectUrl = UriComponentsBuilder
                    .fromUriString(normalizeBaseUrl(frontendBaseUrl) + "/auth/google/callback")
                    .queryParam("ticket", ticket)
                    .build()
                    .encode()
                    .toUriString();

            response.sendRedirect(redirectUrl);
        } catch (RuntimeException ex) {
            log.warn("Google OAuth login provisioning failed for email={}: {}", email, ex.getMessage(), ex);
            redirectWithError(response, ex.getMessage());
        }
    }

    private void redirectWithError(HttpServletResponse response, String error) throws IOException {
        String redirectUrl = UriComponentsBuilder
                .fromUriString(normalizeBaseUrl(frontendBaseUrl) + "/auth/google/callback")
                .queryParam("error", sanitizeErrorMessage(error))
                .build()
                .encode()
                .toUriString();
        response.sendRedirect(redirectUrl);
    }

    private String sanitizeErrorMessage(String error) {
        if (error == null || error.isBlank()) {
            return "Google sign-in failed.";
        }
        return error.trim();
    }

    private String normalizeBaseUrl(String value) {
        return PublicUrlUtils.normalizeBaseUrl(value, "app.public.frontend-base-url");
    }
}
