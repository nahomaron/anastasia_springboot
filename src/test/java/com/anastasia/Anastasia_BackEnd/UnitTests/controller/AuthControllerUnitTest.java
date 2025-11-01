package com.anastasia.Anastasia_BackEnd.UnitTests.controller;

import com.anastasia.Anastasia_BackEnd.common.config.RateLimiterConfig;
import com.anastasia.Anastasia_BackEnd.core.auth.controller.AuthController;
import com.anastasia.Anastasia_BackEnd.core.auth.dto.AuthenticationRequest;
import com.anastasia.Anastasia_BackEnd.core.auth.dto.AuthenticationResponse;
import com.anastasia.Anastasia_BackEnd.core.auth.dto.ResetPasswordRequest;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserDTO;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.core.auth.service.AuthService;
import com.anastasia.Anastasia_BackEnd.modules.users.service.UserService;
import io.github.bucket4j.Bucket;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerUnitTest {

    @Mock
    private AuthService authService;
    @Mock
    private UserService userService;
    @Mock
    private RateLimiterConfig rateLimiterConfig;

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
    }

    @Test
    void signUp_withMatchingPasswords_shouldCreateUser() throws MessagingException {
        UserEntity entity = UserEntity.builder().email(userDTO.getEmail()).build();
        when(userService.convertToEntity(userDTO)).thenReturn(entity);

        ResponseEntity<?> response = authController.signUp(userDTO);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(authService).createUser(entity);
    }

    @Test
    void signUp_whenPasswordsMismatch_shouldReturnBadRequest() throws MessagingException {
        userDTO.setConfirmPassword("Mismatch123!");

        ResponseEntity<?> response = authController.signUp(userDTO);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(authService, org.mockito.Mockito.never()).createUser(any());
    }

    @Test
    void login_shouldDelegateToAuthService() throws MessagingException {
        AuthenticationRequest request = new AuthenticationRequest("test@example.com", "secret");
        AuthenticationResponse expected = AuthenticationResponse.builder()
                .accessToken("access")
                .refreshToken("refresh")
                .build();

        when(authService.authenticate(request)).thenReturn(expected);

        ResponseEntity<AuthenticationResponse> response = authController.login(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
    }

    @Test
    void refreshToken_whenAllowed_shouldInvokeAuthService() {
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        HttpServletResponse httpResponse = mock(HttpServletResponse.class);
        Bucket bucket = mock(Bucket.class);

        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(rateLimiterConfig.getBucket("127.0.0.1")).thenReturn(bucket);
        when(bucket.tryConsume(1)).thenReturn(true);

        ResponseEntity<?> response = authController.refreshToken(httpRequest, httpResponse);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(authService).refreshToken(httpRequest, httpResponse);
    }

    @Test
    void refreshToken_whenRateLimited_shouldReturnTooManyRequests() {
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        HttpServletResponse httpResponse = mock(HttpServletResponse.class);
        Bucket bucket = mock(Bucket.class);

        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(rateLimiterConfig.getBucket("127.0.0.1")).thenReturn(bucket);
        when(bucket.tryConsume(1)).thenReturn(false);

        ResponseEntity<?> response = authController.refreshToken(httpRequest, httpResponse);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getBody()).isEqualTo("Too many requests, try again later");
        verify(authService, org.mockito.Mockito.never()).refreshToken(any(), any());
    }

    @Test
    void confirm_shouldActivateAccount() {
        ResponseEntity<String> response = authController.confirm("token-123");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("successfully activated");
        verify(authService).activateAccount("token-123");
    }

    @Test
    void resendActivation_whenUserMissing_shouldReturnNotFound() throws MessagingException {
        doThrow(new IllegalArgumentException("Not found"))
                .when(authService).resendActivationEmail("missing@mail.com");

        ResponseEntity<String> response = authController.resendActivation("missing@mail.com");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isEqualTo("Not found");
    }

    @Test
    void resendActivation_whenAlreadyVerified_shouldReturnBadRequest() throws MessagingException {
        doThrow(new IllegalStateException("Already verified"))
                .when(authService).resendActivationEmail("user@mail.com");

        ResponseEntity<String> response = authController.resendActivation("user@mail.com");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("Already verified");
    }

    @Test
    void resendActivation_whenMailFails_shouldReturnServerError() throws MessagingException {
        doThrow(new MessagingException("SMTP down")).when(authService).resendActivationEmail("user@mail.com");

        ResponseEntity<String> response = authController.resendActivation("user@mail.com");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isEqualTo("Failed to send activation email");
    }

    @Test
    void forgotPassword_whenEmailMissing_shouldReturnBadRequest() throws MessagingException {
        ResponseEntity<String> response = authController.forgotPassword(Map.of());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("Email is required");
        verify(authService, org.mockito.Mockito.never()).initiatePasswordReset(any());
    }

    @Test
    void forgotPassword_whenEmailProvided_shouldInitiateReset() throws MessagingException {
        ResponseEntity<String> response = authController.forgotPassword(Map.of("email", "user@mail.com"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(authService).initiatePasswordReset("user@mail.com");
    }

    @Test
    void resetPassword_whenPasswordsMismatch_shouldReturnBadRequest() {
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .token("token")
                .newPassword("Password1!")
                .confirmNewPassword("Mismatch1!")
                .build();

        ResponseEntity<String> response = authController.resetPassword(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(authService, org.mockito.Mockito.never()).resetPassword(any(), any());
    }

    @Test
    void resetPassword_whenValid_shouldInvokeService() {
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .token("token")
                .newPassword("Password1!")
                .confirmNewPassword("Password1!")
                .build();

        ResponseEntity<String> response = authController.resetPassword(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(authService).resetPassword("token", "Password1!");
    }

    @Test
    void checkEmail_whenRegistered_shouldReturnRegisteredMessage() {
        when(authService.isEmailRegistered("user@mail.com")).thenReturn(true);

        ResponseEntity<String> response = authController.checkEmail("user@mail.com");

        assertThat(response.getBody()).isEqualTo("Email is already registered.");
    }

    @Test
    void checkEmail_whenAvailable_shouldReturnAvailableMessage() {
        when(authService.isEmailRegistered("free@mail.com")).thenReturn(false);

        ResponseEntity<String> response = authController.checkEmail("free@mail.com");

        assertThat(response.getBody()).isEqualTo("Email is available for registration.");
    }
}
