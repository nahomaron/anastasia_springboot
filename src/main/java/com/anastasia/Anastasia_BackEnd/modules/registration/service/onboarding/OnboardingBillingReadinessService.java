package com.anastasia.Anastasia_BackEnd.modules.registration.service.onboarding;

import com.anastasia.Anastasia_BackEnd.modules.payments.stripe.StripeReadinessService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OnboardingBillingReadinessService {

    private final Environment environment;
    private final StripeReadinessService stripeReadinessService;

    public Map<String, Object> runtimeReadiness() {
        Map<String, Object> stripe = stripeReadinessService.onboardingReadiness();

        boolean frontendBaseUrlConfigured = isSet("app.public.frontend-base-url");
        boolean backendBaseUrlConfigured = isSet("app.public.backend-base-url");
        boolean corsConfigured = isSet("app.cors.allowed-origins");
        boolean jwtSecretConfigured = isSet("app.auth.jwt-current-secret");
        boolean platformAdminBootstrapEnabled = environment.getProperty("app.platform-admin.bootstrap-enabled", Boolean.class, false);
        boolean platformAdminSecretConfigured = isSet("app.platform-admin.secret");
        boolean mailSenderConfigured = isSet("spring.mail.from");
        boolean mailCredentialsConfigured = isSet("spring.mail.username") && isSet("spring.mail.password");
        boolean s3BucketConfigured = isSet("aws.s3.bucket");
        boolean refreshCookieSecure = environment.getProperty("app.auth.refresh-cookie.secure", Boolean.class, false);
        String refreshCookieSameSite = environment.getProperty("app.auth.refresh-cookie.same-site", "Lax");
        boolean notificationEmailEnabled = environment.getProperty("notification.email.enabled", Boolean.class, true);

        List<String> missing = new ArrayList<>();
        if (!frontendBaseUrlConfigured) {
            missing.add("app.public.frontend-base-url (env: APP_FRONTEND_BASE_URL)");
        }
        if (!backendBaseUrlConfigured) {
            missing.add("app.public.backend-base-url (env: APP_BACKEND_BASE_URL)");
        }
        if (!corsConfigured) {
            missing.add("app.cors.allowed-origins (env: APP_CORS_ALLOWED_ORIGINS)");
        }
        if (!jwtSecretConfigured) {
            missing.add("app.auth.jwt-current-secret (env: ANASTASIA_JWT_CURRENT_SECRET)");
        }
        if (platformAdminBootstrapEnabled && !platformAdminSecretConfigured) {
            missing.add("app.platform-admin.secret (env: PLATFORM_ADMIN_SECRET)");
        }
        if (!mailSenderConfigured) {
            missing.add("spring.mail.from (env: MAIL_FROM)");
        }
        if (!mailCredentialsConfigured) {
            missing.add("spring.mail.username/password (env: MAIL_USERNAME, MAIL_PASSWORD)");
        }
        if (!s3BucketConfigured) {
            missing.add("aws.s3.bucket (env: AWS_S3_BUCKET)");
        }
        if (!refreshCookieSecure) {
            missing.add("app.auth.refresh-cookie.secure should be true outside local development");
        }
        if (!"None".equalsIgnoreCase(refreshCookieSameSite)) {
            missing.add("app.auth.refresh-cookie.same-site should be None for cross-site staging login");
        }
        if (!notificationEmailEnabled) {
            missing.add("notification.email.enabled should remain true for onboarding email flows");
        }

        Map<String, Object> application = new LinkedHashMap<>();
        application.put("environment", environment.getProperty("app.env", "unknown"));
        application.put("frontendBaseUrlConfigured", frontendBaseUrlConfigured);
        application.put("backendBaseUrlConfigured", backendBaseUrlConfigured);
        application.put("corsConfigured", corsConfigured);
        application.put("jwtSecretConfigured", jwtSecretConfigured);
        application.put("platformAdminBootstrapEnabled", platformAdminBootstrapEnabled);
        application.put("platformAdminSecretConfigured", platformAdminSecretConfigured);
        application.put("refreshCookieSecure", refreshCookieSecure);
        application.put("refreshCookieSameSite", refreshCookieSameSite);

        Map<String, Object> delivery = new LinkedHashMap<>();
        delivery.put("notificationEmailEnabled", notificationEmailEnabled);
        delivery.put("mailSenderConfigured", mailSenderConfigured);
        delivery.put("mailCredentialsConfigured", mailCredentialsConfigured);

        Map<String, Object> storage = new LinkedHashMap<>();
        storage.put("s3BucketConfigured", s3BucketConfigured);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("ready", missing.isEmpty() && Boolean.TRUE.equals(stripe.get("stripeConfigured")));
        response.put("application", application);
        response.put("delivery", delivery);
        response.put("storage", storage);
        response.put("stripe", stripe);
        response.put("missing", missing);
        return response;
    }

    private boolean isSet(String propertyName) {
        String value = environment.getProperty(propertyName);
        return value != null && !value.isBlank();
    }
}
