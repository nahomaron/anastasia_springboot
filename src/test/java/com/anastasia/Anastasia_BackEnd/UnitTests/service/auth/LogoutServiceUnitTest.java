package com.anastasia.Anastasia_BackEnd.UnitTests.service.auth;

import com.anastasia.Anastasia_BackEnd.UnitTests.support.LenientMockitoTest;
import com.anastasia.Anastasia_BackEnd.common.utils.JwtUtil;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.TokenRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.service.LogoutService;
import com.anastasia.Anastasia_BackEnd.core.auth.service.RefreshTokenCookieService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@LenientMockitoTest
class LogoutServiceUnitTest {

    @Mock
    private TokenRepository tokenRepository;

    @Mock
    private RefreshTokenCookieService refreshTokenCookieService;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private LogoutService logoutService;

    @Test
    void logout_shouldRevokeWholeSessionUsingSessionId() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer access-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(refreshTokenCookieService.extractRefreshToken(request)).thenReturn(Optional.of("refresh-token"));
        when(jwtUtil.extractSessionId("access-token")).thenReturn("session-1");

        logoutService.logout(request, response, null);

        verify(tokenRepository).revokeAllActiveTokensBySessionId(eq("session-1"), any(Instant.class));
        verify(tokenRepository, never()).revokeTokenByJwtId(eq("access-jti"), any());
        verify(refreshTokenCookieService).clearRefreshTokenCookie(response);
    }

    @Test
    void logout_shouldFallbackToJwtIdWhenSessionIdMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer access-token");
        request.setCookies(new Cookie(RefreshTokenCookieService.REFRESH_TOKEN_COOKIE_NAME, "refresh-token"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(refreshTokenCookieService.extractRefreshToken(request)).thenReturn(Optional.of("refresh-token"));
        when(jwtUtil.extractSessionId("access-token")).thenThrow(new IllegalArgumentException("bad access"));
        when(jwtUtil.extractSessionId("refresh-token")).thenThrow(new IllegalArgumentException("bad refresh"));
        when(jwtUtil.extractJwtId("access-token")).thenReturn("access-jti");
        when(jwtUtil.extractJwtId("refresh-token")).thenReturn("refresh-jti");

        logoutService.logout(request, response, null);

        verify(tokenRepository).revokeTokenByJwtId(eq("access-jti"), any(Instant.class));
        verify(tokenRepository).revokeTokenByJwtId(eq("refresh-jti"), any(Instant.class));
        verify(tokenRepository, never()).revokeAllActiveTokensBySessionId(any(), any());
    }
}
