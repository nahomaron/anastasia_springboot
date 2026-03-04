package com.anastasia.Anastasia_BackEnd.modules.payments.stripe;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.core.env.Environment;

import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StripeReadinessService {

    private final Environment environment;

    @Value("${stripe.webhook-secret:}")
    private String stripeWebhookSecret;

    @Value("${billing.tenant.plans.BASIC.price-id:}")
    private String basicPriceId;

    @Value("${billing.tenant.plans.ADVANCED.price-id:}")
    private String advancedPriceId;

    @Value("${billing.tenant.plans.PREMIUM.price-id:}")
    private String premiumPriceId;

    public Map<String, Object> onboardingReadiness() {
        String resolvedApiKey = resolveApiKey();
        boolean apiKeyConfigured = isSet(resolvedApiKey);
        boolean webhookSecretConfigured = isSet(stripeWebhookSecret);

        Map<String, Boolean> planPriceIdsConfigured = new LinkedHashMap<>();
        planPriceIdsConfigured.put("BASIC", isSet(basicPriceId));
        planPriceIdsConfigured.put("ADVANCED", isSet(advancedPriceId));
        planPriceIdsConfigured.put("PREMIUM", isSet(premiumPriceId));

        List<String> missing = new ArrayList<>();
        if (!apiKeyConfigured) {
            missing.add("stripe.api-key (env: STRIPE_API_KEY)");
        }
        if (!webhookSecretConfigured) {
            missing.add("stripe.webhook-secret (env: STRIPE_WEBHOOK_SECRET)");
        }
        if (!planPriceIdsConfigured.get("BASIC")) {
            missing.add("billing.tenant.plans.BASIC.price-id (env: STRIPE_TENANT_BASIC_PRICE_ID)");
        }
        if (!planPriceIdsConfigured.get("ADVANCED")) {
            missing.add("billing.tenant.plans.ADVANCED.price-id (env: STRIPE_TENANT_ADVANCED_PRICE_ID)");
        }
        if (!planPriceIdsConfigured.get("PREMIUM")) {
            missing.add("billing.tenant.plans.PREMIUM.price-id (env: STRIPE_TENANT_PREMIUM_PRICE_ID)");
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("stripeConfigured", missing.isEmpty());
        response.put("apiKeyConfigured", apiKeyConfigured);
        response.put("webhookSecretConfigured", webhookSecretConfigured);
        response.put("planPriceIdsConfigured", planPriceIdsConfigured);
        response.put("missing", missing);
        return response;
    }

    private String resolveApiKey() {
        String apiKey = environment.getProperty("stripe.api-key");
        if (isSet(apiKey)) {
            return apiKey;
        }
        return environment.getProperty("stripe.secret-key");
    }

    private boolean isSet(String value) {
        return value != null && !value.isBlank();
    }
}
