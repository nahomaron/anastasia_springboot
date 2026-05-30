package com.anastasia.Anastasia_BackEnd.UnitTests.controller;

import com.anastasia.Anastasia_BackEnd.common.utils.RateLimiterService;
import com.anastasia.Anastasia_BackEnd.core.auth.controller.AuthController;
import com.anastasia.Anastasia_BackEnd.core.auth.dto.AuthenticationRequest;
import com.anastasia.Anastasia_BackEnd.core.auth.dto.AuthenticationResponse;
import com.anastasia.Anastasia_BackEnd.core.auth.dto.ResetPasswordRequest;
import com.anastasia.Anastasia_BackEnd.core.auth.service.OAuthLoginTicketService;
import com.anastasia.Anastasia_BackEnd.core.auth.service.RefreshTokenCookieService;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserDTO;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.core.auth.service.AuthService;
import com.anastasia.Anastasia_BackEnd.modules.users.service.UserService;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import com.anastasia.Anastasia_BackEnd.UnitTests.support.LenientMockitoTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@LenientMockitoTest
class AuthControllerUnitTest {

    @Mock
    private AuthService authService;
    @Mock
    private UserService userService;
    @Mock
    private RateLimiterService rateLimiterService;
    @Mock
    private OAuthLoginTicketService oauthLoginTicketService;
    @Mock
    private RefreshTokenCookieService refreshTokenCookieService;

    @InjectMocks
    private AuthController authController;

    private UserDTO userDTO;

    @BeforeEach
    void setUp() {
        userDTO = UserDTO.builder()
                .fullName("Test User")
                .email("test@example.com")
                .password("Password1!")
                .confirmPassword("Password1!")
                .build();
        lenient().when(rateLimiterService.tryConsume(anyString(), anyLong(), any(Duration.class))).thenReturn(true);
    }

    @Test
    void signUp_withMatchingPasswords_shouldCreateUser() throws MessagingException {
        UserEntity entity = UserEntity.builder().email(userDTO.getEmail()).build();
        when(userService.convertToEntity(userDTO)).thenReturn(entity);

        ResponseEntity<Map<String, String>> response = authController.signUp(userDTO);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(authService).createUser(entity);
    }

    @Test
    void signUp_whenPasswordsMismatch_shouldReturnBadRequest() throws MessagingException {
        userDTO.setConfirmPassword("Mismatch123!");

        ResponseEntity<Map<String, String>> response = authController.signUp(userDTO);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(authService, org.mockito.Mockito.never()).createUser(any());
    }

    @Test
    void login_shouldDelegateToAuthService() throws MessagingException {
        AuthenticationRequest request = new AuthenticationRequest("test@example.com", "secret");
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        HttpServletResponse httpResponse = mock(HttpServletResponse.class);
        AuthenticationResponse expected = AuthenticationResponse.builder()
                .accessToken("access")
                .refreshToken("refresh")
                .build();

        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(rateLimiterService.tryConsume(eq("auth:login:127.0.0.1:test@example.com"), eq(10L), eq(Duration.ofMinutes(10))))
                .thenReturn(true);
        when(authService.authenticate(request)).thenReturn(expected);

        ResponseEntity<AuthenticationResponse> response = authController.login(request, httpRequest, httpResponse);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(expected);
        assertThat(response.getBody().getAccessToken()).isEqualTo("access");
        assertThat(response.getBody().getRefreshToken()).isNull();
        verify(refreshTokenCookieService).addRefreshTokenCookie(httpResponse, "refresh");
    }

    @Test
    void refreshToken_whenAllowed_shouldInvokeAuthService() {
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        HttpServletResponse httpResponse = mock(HttpServletResponse.class);
        AuthenticationResponse expected = AuthenticationResponse.builder()
                .accessToken("fresh-access")
                .build();

        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(rateLimiterService.tryConsume("127.0.0.1", 5L, Duration.ofMinutes(1))).thenReturn(true);
        when(authService.refreshToken(httpRequest)).thenReturn(expected);

        ResponseEntity<?> response = authController.refreshToken(httpRequest, httpResponse);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
        verify(authService).refreshToken(httpRequest);
        verify(refreshTokenCookieService, never()).addRefreshTokenCookie(any(), any());
    }

    @Test
    void refreshToken_whenRateLimited_shouldReturnTooManyRequests() {
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        HttpServletResponse httpResponse = mock(HttpServletResponse.class);

        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(rateLimiterService.tryConsume("127.0.0.1", 5L, Duration.ofMinutes(1))).thenReturn(false);

        ResponseEntity<?> response = authController.refreshToken(httpRequest, httpResponse);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getBody()).isEqualTo(Map.of("message", "Too many requests, try again later"));
        verify(authService, never()).refreshToken(any());
    }

    @Test
    void confirm_shouldActivateAccount() {
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        HttpServletResponse httpResponse = mock(HttpServletResponse.class);
        AuthenticationResponse expected = AuthenticationResponse.builder()
                .accessToken("access")
                .refreshToken("refresh")
                .build();
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(rateLimiterService.tryConsume(eq("auth:activate-account:127.0.0.1:user@mail.com"), eq(5L), eq(Duration.ofMinutes(15))))
                .thenReturn(true);
        when(authService.activateAccount("token-123", "user@mail.com")).thenReturn(expected);

        ResponseEntity<?> response = authController.confirm("token-123", "user@mail.com", httpRequest, httpResponse);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
        assertThat(expected.getRefreshToken()).isNull();
        verify(authService).activateAccount("token-123", "user@mail.com");
        verify(refreshTokenCookieService).addRefreshTokenCookie(httpResponse, "refresh");
    }

    @Test
    void resendActivation_whenUserMissing_shouldReturnNotFound() throws MessagingException {
        doThrow(new IllegalArgumentException("Not found"))
                .when(authService).resendActivationEmail("missing@mail.com");

        ResponseEntity<Map<String, String>> response = authController.resendActivation("missing@mail.com");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("message", "Not found");
    }

    @Test
    void resendActivation_whenAlreadyVerified_shouldReturnBadRequest() throws MessagingException {
        doThrow(new IllegalStateException("Already verified"))
                .when(authService).resendActivationEmail("user@mail.com");

        ResponseEntity<Map<String, String>> response = authController.resendActivation("user@mail.com");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("message", "Already verified");
    }

    @Test
    void resendActivation_whenMailFails_shouldReturnServerError() throws MessagingException {
        doThrow(new MessagingException("SMTP down")).when(authService).resendActivationEmail("user@mail.com");

        ResponseEntity<Map<String, String>> response = authController.resendActivation("user@mail.com");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsEntry("message", "Failed to send activation email");
    }

    @Test
    void forgotPassword_whenEmailMissing_shouldReturnBadRequest() throws MessagingException {
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        ResponseEntity<Map<String, String>> response = authController.forgotPassword(Map.of(), httpRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("message", "Email is required for password reset.");
        verify(authService, org.mockito.Mockito.never()).initiatePasswordReset(any());
    }

    @Test
    void forgotPassword_whenEmailProvided_shouldInitiateReset() throws MessagingException {
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(rateLimiterService.tryConsume(eq("auth:forgot-password:127.0.0.1:user@mail.com"), eq(3L), eq(Duration.ofMinutes(15))))
                .thenReturn(true);

        ResponseEntity<Map<String, String>> response = authController.forgotPassword(Map.of("email", "user@mail.com"), httpRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(authService).initiatePasswordReset("user@mail.com");
    }

    @Test
    void resetPassword_whenPasswordsMismatch_shouldReturnBadRequest() {
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .token("token")
                .newPassword("Password1!")
                .confirmNewPassword("Mismatch1!")
                .build();

        ResponseEntity<Map<String, String>> response = authController.resetPassword(request, httpRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(authService, org.mockito.Mockito.never()).resetPassword(any(), any());
    }

    @Test
    void resetPassword_whenValid_shouldInvokeService() {
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .token("token")
                .newPassword("Password1!")
                .confirmNewPassword("Password1!")
                .build();

        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(rateLimiterService.tryConsume(eq("auth:reset-password:127.0.0.1:token"), eq(5L), eq(Duration.ofMinutes(15))))
                .thenReturn(true);

        ResponseEntity<Map<String, String>> response = authController.resetPassword(request, httpRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(authService).resetPassword("token", "Password1!");
    }

    @Test
    void checkEmail_whenRegistered_shouldReturnRegisteredMessage() {
        when(authService.isEmailRegistered("user@mail.com")).thenReturn(true);

        ResponseEntity<Map<String, Object>> response = authController.checkEmail("user@mail.com");

        assertThat(response.getBody()).containsEntry("registered", true);
        assertThat(response.getBody()).containsEntry("message", "Email is already registered.");
    }

    @Test
    void checkEmail_whenAvailable_shouldReturnAvailableMessage() {
        when(authService.isEmailRegistered("free@mail.com")).thenReturn(false);

        ResponseEntity<Map<String, Object>> response = authController.checkEmail("free@mail.com");

        assertThat(response.getBody()).containsEntry("registered", false);
        assertThat(response.getBody()).containsEntry("message", "Email is available for registration.");
    }
}
