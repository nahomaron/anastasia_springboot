package com.anastasia.Anastasia_BackEnd.UnitTests.security;

import com.anastasia.Anastasia_BackEnd.common.security.OAuth2AuthenticationFailureHandler;
import com.anastasia.Anastasia_BackEnd.common.security.OAuth2AuthenticationSuccessHandler;
import com.anastasia.Anastasia_BackEnd.core.auth.dto.AuthenticationResponse;
import com.anastasia.Anastasia_BackEnd.core.auth.service.AuthService;
import com.anastasia.Anastasia_BackEnd.core.auth.service.OAuthLoginTicketService;
import com.anastasia.Anastasia_BackEnd.UnitTests.support.LenientMockitoTest;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@LenientMockitoTest
class OAuth2AuthenticationHandlerTest {

    @Mock
    private AuthService authService;

    @Mock
    private OAuthLoginTicketService ticketService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private OAuth2AuthenticationSuccessHandler successHandler;

    @InjectMocks
    private OAuth2AuthenticationFailureHandler failureHandler;

    @Test
    void successHandler_shouldRedirectToTicketCallbackOnSuccessfulProvisioning() throws Exception {
        OAuth2User oauthUser = oauthUser(
                "google-123",
                "test@example.com",
                "Test User",
                true
        );

        when(authentication.getPrincipal()).thenReturn(oauthUser);
        when(authService.authenticateGoogleUser("google-123", "test@example.com", "Test User"))
                .thenReturn(AuthenticationResponse.builder().accessToken("access").build());
        when(ticketService.store(org.mockito.ArgumentMatchers.any(AuthenticationResponse.class))).thenReturn("ticket-123");
        ReflectionTestUtils.setField(successHandler, "frontendBaseUrl", "https://staging.anastasisapp.com");

        MockHttpServletResponse response = new MockHttpServletResponse();

        successHandler.onAuthenticationSuccess(new MockHttpServletRequest(), response, authentication);

        assertThat(response.getRedirectedUrl())
                .isEqualTo("https://staging.anastasisapp.com/auth/google/callback?ticket=ticket-123");
        verify(ticketService).store(org.mockito.ArgumentMatchers.any(AuthenticationResponse.class));
    }

    @Test
    void successHandler_shouldEncodeErrorMessageWhenProvisioningFails() throws Exception {
        OAuth2User oauthUser = oauthUser(
                "google-123",
                "test@example.com",
                "Test User",
                true
        );

        when(authentication.getPrincipal()).thenReturn(oauthUser);
        when(authService.authenticateGoogleUser("google-123", "test@example.com", "Test User"))
                .thenThrow(new IllegalStateException("detached entity passed to persist: com.example.UserEntity"));
        ReflectionTestUtils.setField(successHandler, "frontendBaseUrl", "https://staging.anastasisapp.com");

        MockHttpServletResponse response = new MockHttpServletResponse();

        successHandler.onAuthenticationSuccess(new MockHttpServletRequest(), response, authentication);

        assertThat(response.getRedirectedUrl())
                .isEqualTo("https://staging.anastasisapp.com/auth/google/callback?error=detached%20entity%20passed%20to%20persist:%20com.example.UserEntity");
    }

    @Test
    void failureHandler_shouldEncodeProviderErrorMessage() throws Exception {
        ReflectionTestUtils.setField(failureHandler, "frontendBaseUrl", "https://staging.anastasisapp.com");
        MockHttpServletResponse response = new MockHttpServletResponse();

        failureHandler.onAuthenticationFailure(
                new MockHttpServletRequest(),
                response,
                new BadCredentialsException("oauth provider rejected callback with invalid state")
        );

        assertThat(response.getRedirectedUrl())
                .isEqualTo("https://staging.anastasisapp.com/auth/google/callback?error=oauth%20provider%20rejected%20callback%20with%20invalid%20state");
    }

    @Test
    void failureHandler_shouldPreferProviderDescriptionWhenExceptionMessageIsBlank() throws Exception {
        ReflectionTestUtils.setField(failureHandler, "frontendBaseUrl", "https://staging.anastasisapp.com");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("error", "access_denied");
        request.setParameter("error_description", "User cancelled the Google consent screen");
        MockHttpServletResponse response = new MockHttpServletResponse();

        failureHandler.onAuthenticationFailure(
                request,
                response,
                new OAuth2AuthenticationException(new OAuth2Error("access_denied", null, null), " ")
        );

        assertThat(response.getRedirectedUrl())
                .isEqualTo("https://staging.anastasisapp.com/auth/google/callback?error=User%20cancelled%20the%20Google%20consent%20screen");
    }

    @Test
    void successHandler_shouldUseRootCauseMessageWhenProvisioningExceptionMessageIsBlank() throws Exception {
        OAuth2User oauthUser = oauthUser(
                "google-123",
                "test@example.com",
                "Test User",
                true
        );

        when(authentication.getPrincipal()).thenReturn(oauthUser);
        when(authService.authenticateGoogleUser("google-123", "test@example.com", "Test User"))
                .thenThrow(new IllegalStateException(" ", new RuntimeException("database write failed")));
        ReflectionTestUtils.setField(successHandler, "frontendBaseUrl", "https://staging.anastasisapp.com");

        MockHttpServletResponse response = new MockHttpServletResponse();

        successHandler.onAuthenticationSuccess(new MockHttpServletRequest(), response, authentication);

        assertThat(response.getRedirectedUrl())
                .isEqualTo("https://staging.anastasisapp.com/auth/google/callback?error=database%20write%20failed");
    }

    private OAuth2User oauthUser(String sub, String email, String name, boolean emailVerified) {
        return new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of(
                        "sub", sub,
                        "email", email,
                        "name", name,
                        "email_verified", emailVerified
                ),
                "sub"
        );
    }
}
