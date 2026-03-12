package com.anastasia.Anastasia_BackEnd.core.auth.service;

import com.anastasia.Anastasia_BackEnd.core.auth.repository.TokenRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LogoutService implements LogoutHandler {

    private final TokenRepository tokenRepository;
    private final RefreshTokenCookieService refreshTokenCookieService;

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        final String authHeader = request.getHeader("Authorization");
        String accessToken = null;

        if(authHeader != null && authHeader.startsWith("Bearer ")){
            accessToken = authHeader.substring(7);
        }

        revokeToken(accessToken);
        refreshTokenCookieService.extractRefreshToken(request).ifPresent(this::revokeToken);
        refreshTokenCookieService.clearRefreshTokenCookie(response);
    }

    private void revokeToken(String token) {
        if (token == null || token.isBlank()) {
            return;
        }

        var storedToken = tokenRepository.findTopByTokenOrderByIdDesc(token).orElse(null);

        if (storedToken != null) {
            storedToken.setExpired(true);
            storedToken.setRevoked(true);
            storedToken.setExpiredAt(java.time.Instant.now());
            storedToken.setRevokedAt(java.time.Instant.now());
            tokenRepository.save(storedToken);
        }
    }
}
