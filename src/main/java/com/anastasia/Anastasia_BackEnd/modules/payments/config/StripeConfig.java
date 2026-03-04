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
            return new StripeKeyResolution("stripe.api-key", apiKey.trim());
        }

        String secretKey = environment.getProperty("stripe.secret-key");
        if (secretKey != null && !secretKey.isBlank()) {
            return new StripeKeyResolution("stripe.secret-key", secretKey.trim());
        }

        return new StripeKeyResolution("none", null);
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
