package com.anastasia.Anastasia_BackEnd.UnitTests.service.onboarding;

import com.anastasia.Anastasia_BackEnd.UnitTests.support.LenientMockitoTest;
import com.anastasia.Anastasia_BackEnd.common.utils.RateLimiterService;
import com.anastasia.Anastasia_BackEnd.modules.registration.controller.OnboardingEmailVerificationController;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.onboarding.OnboardingEmailVerificationService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@LenientMockitoTest
class OnboardingEmailVerificationControllerTest {

    @Mock
    private OnboardingEmailVerificationService verificationService;

    @Mock
    private RateLimiterService rateLimiterService;

    @InjectMocks
    private OnboardingEmailVerificationController controller;

    @Test
    void sendCode_whenRateLimited_shouldReturnTooManyRequests() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(rateLimiterService.tryConsume(
                eq("onboarding:email-verification:send:127.0.0.1:owner@example.com"),
                eq(3L),
                eq(Duration.ofMinutes(15))
        )).thenReturn(false);

        ResponseEntity<Map<String, String>> response = controller.sendCode(
                new OnboardingEmailVerificationController.SendCodeRequest("owner@example.com"),
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getBody()).containsEntry("message", "Too many requests, try again later");
    }

    @Test
    void verifyCode_whenInvalid_shouldReturnGenericFailure() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(rateLimiterService.tryConsume(
                eq("onboarding:email-verification:verify:127.0.0.1:owner@example.com"),
                eq(5L),
                eq(Duration.ofMinutes(10))
        )).thenReturn(true);
        when(verificationService.verifyCode("owner@example.com", "123456")).thenReturn(false);

        ResponseEntity<Map<String, Object>> response = controller.verifyCode(
                new OnboardingEmailVerificationController.VerifyCodeRequest("owner@example.com", "123456"),
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("verified", false);
        assertThat(response.getBody()).containsEntry("message", "Unable to verify email with the provided code.");
        verify(verificationService).verifyCode("owner@example.com", "123456");
    }
}
