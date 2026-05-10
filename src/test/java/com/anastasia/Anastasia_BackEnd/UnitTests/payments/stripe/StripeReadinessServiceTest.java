package com.anastasia.Anastasia_BackEnd.UnitTests.payments.stripe;

import com.anastasia.Anastasia_BackEnd.modules.payments.stripe.StripeReadinessService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class StripeReadinessServiceTest {

    @Test
    void reportsConfiguredWhenApiKeyWebhookAndAllPlanPricesExist() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("stripe.secret-key", "sk_test_123")
                .withProperty("stripe.webhook-secret", "whsec_123")
                .withProperty("billing.tenant.plans.BASIC.price-id", "price_basic")
                .withProperty("billing.tenant.plans.ADVANCED.price-id", "price_advanced")
                .withProperty("billing.tenant.plans.PREMIUM.price-id", "price_premium");

        Map<String, Object> readiness = new StripeReadinessService(environment).onboardingReadiness();

        assertThat(readiness).containsEntry("stripeConfigured", true);
        assertThat((List<String>) readiness.get("missing")).isEmpty();
    }

    @Test
    void reportsMissingPlanIdsIndividually() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("stripe.secret-key", "sk_test_123");

        Map<String, Object> readiness = new StripeReadinessService(environment).onboardingReadiness();

        assertThat(readiness).containsEntry("stripeConfigured", false);
        assertThat((List<String>) readiness.get("missing"))
                .contains("stripe.webhook-secret (env: STRIPE_WEBHOOK_SECRET)")
                .contains("billing.tenant.plans.BASIC.price-id (env: STRIPE_TENANT_BASIC_PRICE_ID)")
                .contains("billing.tenant.plans.ADVANCED.price-id (env: STRIPE_TENANT_ADVANCED_PRICE_ID)")
                .contains("billing.tenant.plans.PREMIUM.price-id (env: STRIPE_TENANT_PREMIUM_PRICE_ID)");
    }
}
