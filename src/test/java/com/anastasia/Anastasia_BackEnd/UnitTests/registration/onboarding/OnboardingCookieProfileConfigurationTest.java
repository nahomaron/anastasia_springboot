package com.anastasia.Anastasia_BackEnd.UnitTests.registration.onboarding;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class OnboardingCookieProfileConfigurationTest {

    @Test
    void defaultsToSecureOnboardingCookieConfiguration() {
        String yaml = loadYaml("application-staging.yml");

        assertThat(yaml).contains("access-cookie:");
        assertThat(yaml).contains("secure: true");
        assertThat(yaml).contains("same-site: ${APP_ONBOARDING_ACCESS_COOKIE_SAME_SITE:None}");
    }

    @Test
    void stagingProfilePinsSecureCrossSiteOnboardingCookie() {
        String yaml = loadYaml("application-staging.yml");

        assertThat(yaml).contains("access-cookie:");
        assertThat(yaml).contains("secure: true");
        assertThat(yaml).contains("same-site: ${APP_ONBOARDING_ACCESS_COOKIE_SAME_SITE:None}");
    }

    @Test
    void productionProfilePinsSecureCrossSiteOnboardingCookie() {
        String yaml = loadYaml("application-prod.yml");

        assertThat(yaml).contains("access-cookie:");
        assertThat(yaml).contains("secure: true");
        assertThat(yaml).contains("same-site: ${APP_ONBOARDING_ACCESS_COOKIE_SAME_SITE:None}");
    }

    private String loadYaml(String resource) {
        try {
            return Files.readString(Path.of("src/main/resources", resource), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to load yaml resource " + resource, ex);
        }
    }
}
