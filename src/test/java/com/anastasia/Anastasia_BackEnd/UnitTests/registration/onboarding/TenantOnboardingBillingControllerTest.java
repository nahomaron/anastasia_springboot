package com.anastasia.Anastasia_BackEnd.UnitTests.registration.onboarding;

import com.anastasia.Anastasia_BackEnd.common.utils.RateLimiterService;
import com.anastasia.Anastasia_BackEnd.core.auth.dto.AuthenticationResponse;
import com.anastasia.Anastasia_BackEnd.core.auth.service.RefreshTokenCookieService;
import com.anastasia.Anastasia_BackEnd.modules.payments.stripe.StripeReadinessService;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.onboarding.OnboardingSessionResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.OnboardingSessionStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.SubscriptionPlan;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantType;
import com.anastasia.Anastasia_BackEnd.modules.registration.controller.TenantOnboardingBillingController;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.onboarding.OnboardingBillingReadinessService;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.onboarding.OnboardingSessionAccessService;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.onboarding.TenantOnboardingBillingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TenantOnboardingBillingControllerTest {

    private TenantOnboardingBillingController controller;
    private TenantOnboardingBillingService onboardingBillingService;
    private StripeReadinessService stripeReadinessService;
    private OnboardingBillingReadinessService onboardingBillingReadinessService;
    private RefreshTokenCookieService refreshTokenCookieService;
    private OnboardingSessionAccessService onboardingSessionAccessService;
    private RateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() {
        onboardingBillingService = mock(TenantOnboardingBillingService.class);
        stripeReadinessService = mock(StripeReadinessService.class);
        onboardingBillingReadinessService = mock(OnboardingBillingReadinessService.class);
        refreshTokenCookieService = mock(RefreshTokenCookieService.class);
        onboardingSessionAccessService = mock(OnboardingSessionAccessService.class);
        rateLimiterService = mock(RateLimiterService.class);
        when(rateLimiterService.tryConsume(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any(Duration.class)
        )).thenReturn(true);
        controller = new TenantOnboardingBillingController(
                onboardingBillingService,
                stripeReadinessService,
                onboardingBillingReadinessService,
                refreshTokenCookieService,
                onboardingSessionAccessService,
                rateLimiterService
        );
    }

    @Test
    void stripeHealthDelegatesToStripeReadinessService() {
        Map<String, Object> expected = Map.of("stripeConfigured", true);
        when(stripeReadinessService.onboardingReadiness()).thenReturn(expected);

        assertThat(controller.stripeHealth().getBody()).isEqualTo(expected);
    }

    @Test
    void runtimeHealthDelegatesToRuntimeReadinessService() {
        Map<String, Object> expected = Map.of("ready", true);
        when(onboardingBillingReadinessService.runtimeReadiness()).thenReturn(expected);

        assertThat(controller.runtimeHealth().getBody()).isEqualTo(expected);
    }

    @Test
    void createOnboardingSessionSetsAccessCookieWhenTokenIssued() {
        OnboardingSessionResponse sessionResponse = OnboardingSessionResponse.builder()
                .sessionId(UUID.randomUUID())
                .status(OnboardingSessionStatus.DRAFT)
                .tenantType(TenantType.CHURCH)
                .selectedPlan(SubscriptionPlan.FREE)
                .expiresAt(Instant.now().plusSeconds(600))
                .onboardingAccessToken("issued-token")
                .build();
        when(onboardingBillingService.createSession(
                org.mockito.ArgumentMatchers.any(TenantDTO.class),
                org.mockito.ArgumentMatchers.eq("idem-1"),
                org.mockito.ArgumentMatchers.isNull()
        ))
                .thenReturn(sessionResponse);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        TenantDTO tenantDTO = new TenantDTO();
        tenantDTO.setOwnerEmail("Owner@Example.com");
        controller.createOnboardingSession("idem-1", tenantDTO, null, request, response);

        verify(rateLimiterService).tryConsume(
                "onboarding:billing:sessions:127.0.0.1:owner@example.com",
                5L,
                Duration.ofMinutes(15)
        );
        verify(onboardingSessionAccessService).addAccessTokenCookie(response, "issued-token", sessionResponse.getExpiresAt());
    }

    @Test
    void createOnboardingSessionReturnsTooManyRequestsWhenRateLimited() {
        when(rateLimiterService.tryConsume(
                "onboarding:billing:sessions:127.0.0.1:owner@example.com",
                5L,
                Duration.ofMinutes(15)
        )).thenReturn(false);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        TenantDTO tenantDTO = new TenantDTO();
        tenantDTO.setOwnerEmail("owner@example.com");

        var result = controller.createOnboardingSession("idem-1", tenantDTO, null, request, response);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        verify(onboardingBillingService, never()).createSession(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void autoLoginAddsRefreshTokenCookie() {
        UUID sessionId = UUID.randomUUID();
        AuthenticationResponse authResponse = AuthenticationResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .build();
        when(onboardingSessionAccessService.extractAccessToken(org.mockito.ArgumentMatchers.any())).thenReturn(java.util.Optional.of("onboarding-token"));
        when(onboardingBillingService.autoLogin(sessionId, "onboarding-token")).thenReturn(authResponse);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AuthenticationResponse body = controller.autoLogin(sessionId, null, request, response).getBody();

        assertThat(body).isNotNull();
        assertThat(body.getRefreshToken()).isNull();
        verify(rateLimiterService).tryConsume(
                "onboarding:billing:auto-login:127.0.0.1:" + sessionId,
                5L,
                Duration.ofMinutes(10)
        );
        verify(refreshTokenCookieService).addRefreshTokenCookie(response, "refresh-token");
    }

    @Test
    void autoLoginReturnsTooManyRequestsWhenRateLimited() {
        UUID sessionId = UUID.randomUUID();
        when(rateLimiterService.tryConsume(
                "onboarding:billing:auto-login:127.0.0.1:" + sessionId,
                5L,
                Duration.ofMinutes(10)
        )).thenReturn(false);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        var result = controller.autoLogin(sessionId, "token", request, response);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        verify(onboardingBillingService, never()).autoLogin(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
        verify(refreshTokenCookieService, never()).addRefreshTokenCookie(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString()
        );
    }

    @Test
    void getSessionDelegatesWithOnboardingAccessTokenFromHeader() {
        UUID sessionId = UUID.randomUUID();
        OnboardingSessionResponse expected = OnboardingSessionResponse.builder()
                .sessionId(sessionId)
                .status(OnboardingSessionStatus.DRAFT)
                .build();
        when(onboardingBillingService.getSession(sessionId, "header-token")).thenReturn(expected);

        var response = controller.getSession(sessionId, "header-token", new MockHttpServletRequest());

        assertThat(response.getBody()).isEqualTo(expected);
        verify(onboardingBillingService).getSession(sessionId, "header-token");
    }

    @Test
    void validateEligibilityDelegatesToBillingService() {
        var response = controller.validateEligibility(
                new TenantOnboardingBillingController.OnboardingEligibilityRequest(
                        "owner@example.com",
                        "+15551234567"
                )
        );

        assertThat(response.getBody()).isEqualTo(Map.of(
                "eligible", true,
                "message", "Owner identity is eligible for onboarding."
        ));
        verify(onboardingBillingService).validateOwnerIdentity("owner@example.com", "+15551234567");
    }
}
