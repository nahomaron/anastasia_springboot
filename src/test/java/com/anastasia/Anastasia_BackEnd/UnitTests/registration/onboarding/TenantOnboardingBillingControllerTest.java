package com.anastasia.Anastasia_BackEnd.UnitTests.registration.onboarding;

import com.anastasia.Anastasia_BackEnd.core.auth.service.RefreshTokenCookieService;
import com.anastasia.Anastasia_BackEnd.modules.payments.stripe.StripeReadinessService;
import com.anastasia.Anastasia_BackEnd.modules.registration.controller.TenantOnboardingBillingController;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.onboarding.OnboardingBillingReadinessService;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.onboarding.TenantOnboardingBillingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TenantOnboardingBillingControllerTest {

    private TenantOnboardingBillingController controller;
    private StripeReadinessService stripeReadinessService;
    private OnboardingBillingReadinessService onboardingBillingReadinessService;

    @BeforeEach
    void setUp() {
        TenantOnboardingBillingService onboardingBillingService = mock(TenantOnboardingBillingService.class);
        stripeReadinessService = mock(StripeReadinessService.class);
        onboardingBillingReadinessService = mock(OnboardingBillingReadinessService.class);
        RefreshTokenCookieService refreshTokenCookieService = mock(RefreshTokenCookieService.class);
        controller = new TenantOnboardingBillingController(
                onboardingBillingService,
                stripeReadinessService,
                onboardingBillingReadinessService,
                refreshTokenCookieService
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
}
