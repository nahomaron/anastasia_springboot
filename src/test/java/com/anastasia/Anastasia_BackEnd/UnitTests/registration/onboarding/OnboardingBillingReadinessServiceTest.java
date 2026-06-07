package com.anastasia.Anastasia_BackEnd.UnitTests.registration.onboarding;

import com.anastasia.Anastasia_BackEnd.modules.payments.stripe.StripeReadinessService;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.onboarding.OnboardingBillingReadinessService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OnboardingBillingReadinessServiceTest {

    @Test
    void reportsReadyWhenStagingCriticalConfigurationIsPresent() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("app.env", "staging")
                .withProperty("app.public.frontend-base-url", "https://staging.anastasisapp.com")
                .withProperty("app.public.backend-base-url", "https://staging-api.anastasisapp.com")
                .withProperty("app.cors.allowed-origins", "https://staging.anastasisapp.com")
                .withProperty("app.auth.jwt-current-secret", "secret")
                .withProperty("app.platform-admin.secret", "platform-secret")
                .withProperty("app.auth.refresh-cookie.secure", "true")
                .withProperty("app.auth.refresh-cookie.same-site", "None")
                .withProperty("app.onboarding.access-cookie.secure", "true")
                .withProperty("app.onboarding.access-cookie.same-site", "None")
                .withProperty("spring.mail.from", "noreply@anastasisapp.com")
                .withProperty("spring.mail.username", "smtp-user")
                .withProperty("spring.mail.password", "smtp-pass")
                .withProperty("aws.s3.bucket", "staging-assets")
                .withProperty("notification.email.enabled", "true")
                .withProperty("stripe.secret-key", "sk_test_123")
                .withProperty("stripe.webhook-secret", "whsec_123")
                .withProperty("billing.tenant.plans.BASIC.price-id", "price_basic")
                .withProperty("billing.tenant.plans.ADVANCED.price-id", "price_advanced")
                .withProperty("billing.tenant.plans.PREMIUM.price-id", "price_premium");

        OnboardingBillingReadinessService service =
                new OnboardingBillingReadinessService(environment, new StripeReadinessService(environment));

        Map<String, Object> readiness = service.runtimeReadiness();

        assertThat(readiness).containsEntry("ready", true);
        assertThat((List<String>) readiness.get("missing")).isEmpty();
    }

    @Test
    void reportsMissingStagingCriticalConfiguration() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("app.env", "staging")
                .withProperty("app.auth.refresh-cookie.secure", "false")
                .withProperty("app.auth.refresh-cookie.same-site", "Lax")
                .withProperty("app.onboarding.access-cookie.secure", "false")
                .withProperty("app.onboarding.access-cookie.same-site", "Lax")
                .withProperty("notification.email.enabled", "false");

        OnboardingBillingReadinessService service =
                new OnboardingBillingReadinessService(environment, new StripeReadinessService(environment));

        Map<String, Object> readiness = service.runtimeReadiness();

        assertThat(readiness).containsEntry("ready", false);
        assertThat((List<String>) readiness.get("missing"))
                .contains("app.public.frontend-base-url (env: APP_FRONTEND_BASE_URL)")
                .contains("app.auth.refresh-cookie.secure should be true outside local development")
                .contains("app.auth.refresh-cookie.same-site should be None for cross-site staging login")
                .contains("app.onboarding.access-cookie.secure should be true outside local development")
                .contains("app.onboarding.access-cookie.same-site should be None for cross-site onboarding over HTTPS")
                .contains("notification.email.enabled should remain true for onboarding email flows");
    }
}
