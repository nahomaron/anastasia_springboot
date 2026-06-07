package com.anastasia.Anastasia_BackEnd.core.auth.service;

import com.anastasia.Anastasia_BackEnd.core.auth.repository.TokenRepository;
import com.anastasia.Anastasia_BackEnd.common.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class LogoutService implements LogoutHandler {

    private final TokenRepository tokenRepository;
    private final RefreshTokenCookieService refreshTokenCookieService;
    private final JwtUtil jwtUtil;

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        final String authHeader = request.getHeader("Authorization");
        String accessToken = null;

        if(authHeader != null && authHeader.startsWith("Bearer ")){
            accessToken = authHeader.substring(7);
        }

        String refreshToken = refreshTokenCookieService.extractRefreshToken(request).orElse(null);

        revokeSessionFamily(accessToken, refreshToken);
        refreshTokenCookieService.clearRefreshTokenCookie(response);
    }

    private void revokeSessionFamily(String accessToken, String refreshToken) {
        Instant now = Instant.now();
        String sessionId = extractSessionId(accessToken);
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = extractSessionId(refreshToken);
        }
        if (sessionId != null && !sessionId.isBlank()) {
            tokenRepository.revokeAllActiveTokensBySessionId(sessionId, now);
            return;
        }

        revokeByJwtId(accessToken, now);
        revokeByJwtId(refreshToken, now);
    }

    private void revokeByJwtId(String token, Instant now) {
        String jwtId = extractJwtId(token);
        if (jwtId == null || jwtId.isBlank()) {
            return;
        }
        tokenRepository.revokeTokenByJwtId(jwtId, now);
    }

    private String extractSessionId(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            return jwtUtil.extractSessionId(token);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String extractJwtId(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            return jwtUtil.extractJwtId(token);
        } catch (Exception ignored) {
            return null;
        }
    }
}
