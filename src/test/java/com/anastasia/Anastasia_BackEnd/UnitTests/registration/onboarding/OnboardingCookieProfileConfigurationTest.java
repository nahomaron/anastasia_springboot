package com.anastasia.Anastasia_BackEnd.UnitTests.registration.onboarding;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class OnboardingCookieProfileConfigurationTest {

    @Test
    void defaultsToSecureOnboardingCookieConfiguration() {
        Properties properties = loadProperties("application.yml");

        assertThat(properties.getProperty("app.onboarding.access-cookie.secure")).isEqualTo("true");
        assertThat(properties.getProperty("app.onboarding.access-cookie.same-site")).isEqualTo("None");
    }

    @Test
    void stagingProfilePinsSecureCrossSiteOnboardingCookie() {
        Properties properties = loadProperties("application.yml", "application-staging.yml");

        assertThat(properties.getProperty("app.onboarding.access-cookie.secure")).isEqualTo("true");
        assertThat(properties.getProperty("app.onboarding.access-cookie.same-site")).isEqualTo("None");
    }

    @Test
    void productionProfilePinsSecureCrossSiteOnboardingCookie() {
        Properties properties = loadProperties("application.yml", "application-prod.yml");

        assertThat(properties.getProperty("app.onboarding.access-cookie.secure")).isEqualTo("true");
        assertThat(properties.getProperty("app.onboarding.access-cookie.same-site")).isEqualTo("None");
    }

    private Properties loadProperties(String... resources) {
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        ClassPathResource[] yamlResources = new ClassPathResource[resources.length];
        for (int i = 0; i < resources.length; i++) {
            yamlResources[i] = new ClassPathResource(resources[i]);
        }
        factory.setResources(yamlResources);
        Properties properties = factory.getObject();
        return properties == null ? new Properties() : properties;
    }
}
