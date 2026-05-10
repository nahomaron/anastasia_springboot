package com.anastasia.Anastasia_BackEnd.modules.payments.stripe;

import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StripeReadinessService {

    private final Environment environment;

    public Map<String, Object> onboardingReadiness() {
        boolean apiKeyConfigured = isSet(resolveApiKey());
        boolean webhookSecretConfigured = isSet(environment.getProperty("stripe.webhook-secret"));

        Map<String, Boolean> planPriceIdsConfigured = new LinkedHashMap<>();
        planPriceIdsConfigured.put("BASIC", isSet(environment.getProperty("billing.tenant.plans.BASIC.price-id")));
        planPriceIdsConfigured.put("ADVANCED", isSet(environment.getProperty("billing.tenant.plans.ADVANCED.price-id")));
        planPriceIdsConfigured.put("PREMIUM", isSet(environment.getProperty("billing.tenant.plans.PREMIUM.price-id")));

        List<String> missing = new ArrayList<>();
        if (!apiKeyConfigured) {
            missing.add("stripe.secret-key (env: STRIPE_SECRET_KEY)");
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
        String secretKey = environment.getProperty("stripe.secret-key");
        if (isSet(secretKey)) {
            return secretKey;
        }
        return environment.getProperty("stripe.api-key");
    }

    private boolean isSet(String value) {
        return value != null && !value.isBlank();
    }
}
