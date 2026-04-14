package com.anastasia.Anastasia_BackEnd.modules.payments.config;

import com.stripe.Stripe;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.Arrays;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class StripeConfig {

    private final Environment environment;
    private static final String TEST_FALLBACK_API_KEY = "sk_" + "test_ci_placeholder_key";

    @PostConstruct
    public void init() {
        StripeKeyResolution keyResolution = resolveApiKey();
        String apiKey = keyResolution.value();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "Missing Stripe API key. Configure stripe.api-key/stripe.secret-key or set STRIPE_API_KEY."
            );
        }
        if (!apiKey.startsWith("sk_")) {
            throw new IllegalStateException(
                    "Invalid Stripe API key format. Backend must use a secret key starting with 'sk_'."
            );
        }
        Stripe.apiKey = apiKey;
        log.info(
                "Stripe init: activeProfiles={}, sourceKeyUsed={}, maskedKeyPrefix={}",
                Arrays.toString(environment.getActiveProfiles()),
                keyResolution.source(),
                maskKey(apiKey)
        );
    }

    private StripeKeyResolution resolveApiKey() {
        String apiKey = environment.getProperty("stripe.api-key");
        if (apiKey != null && !apiKey.isBlank()) {
            String trimmed = apiKey.trim();
            if (isTestProfile() && isTestPlaceholder(trimmed)) {
                return new StripeKeyResolution("test-fallback", TEST_FALLBACK_API_KEY);
            }
            return new StripeKeyResolution("stripe.api-key", trimmed);
        }

        String secretKey = environment.getProperty("stripe.secret-key");
        if (secretKey != null && !secretKey.isBlank()) {
            String trimmed = secretKey.trim();
            if (isTestProfile() && isTestPlaceholder(trimmed)) {
                return new StripeKeyResolution("test-fallback", TEST_FALLBACK_API_KEY);
            }
            return new StripeKeyResolution("stripe.secret-key", trimmed);
        }

        if (isTestProfile()) {
            return new StripeKeyResolution("test-fallback", TEST_FALLBACK_API_KEY);
        }

        return new StripeKeyResolution("none", null);
    }

    private boolean isTestProfile() {
        return Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> "test".equals(profile) || "api-tests".equals(profile));
    }

    private boolean isTestPlaceholder(String value) {
        return value.startsWith("stripe_test_placeholder");
    }

    private String maskKey(String key) {
        if (key == null || key.isBlank()) {
            return "missing";
        }
        int visible = Math.min(7, key.length());
        return key.substring(0, visible) + "...";
    }

    private record StripeKeyResolution(String source, String value) {}
}
