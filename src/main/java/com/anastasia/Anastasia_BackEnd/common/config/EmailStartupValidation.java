package com.anastasia.Anastasia_BackEnd.common.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class EmailStartupValidation {

    private final Environment environment;
    private final boolean validationEnabled;

    public EmailStartupValidation(
            Environment environment,
            @Value("${notification.email.startup-validation.enabled:true}") boolean validationEnabled
    ) {
        this.environment = environment;
        this.validationEnabled = validationEnabled;
    }

    @PostConstruct
    public void validate() {
        if (!validationEnabled || isTestProfile()) {
            return;
        }

        boolean emailEnabled = environment.getProperty("notification.email.enabled", Boolean.class, true);
        if (!emailEnabled) {
            return;
        }

        List<String> missing = new ArrayList<>();
        requireProperty("spring.mail.host", "MAIL_HOST", missing);
        requireProperty("spring.mail.username", "MAIL_USERNAME", missing);
        requireProperty("spring.mail.password", "MAIL_PASSWORD", missing);
        requireProperty("spring.mail.from", "MAIL_FROM", missing);
        requireProperty("app.public.frontend-base-url", "APP_FRONTEND_BASE_URL", missing);

        if (!missing.isEmpty()) {
            String activeProfiles = Arrays.toString(environment.getActiveProfiles());
            throw new IllegalStateException(
                    "Email delivery is enabled but required configuration is missing for profiles "
                            + activeProfiles
                            + ": "
                            + String.join(", ", missing)
            );
        }
    }

    private void requireProperty(String propertyName, String envName, List<String> missing) {
        String value = environment.getProperty(propertyName);
        if (value == null || value.isBlank()) {
            missing.add(propertyName + " (env: " + envName + ")");
        }
    }

    private boolean isTestProfile() {
        return PublicUrlUtils.isLocalProfile(environment);
    }
}
