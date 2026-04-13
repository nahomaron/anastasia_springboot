package com.anastasia.Anastasia_BackEnd.TestSupport;

import com.anastasia.Anastasia_BackEnd.util.TestJwtSecrets;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.LinkedHashMap;
import java.util.Map;

public class TestJwtEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (environment.containsProperty("app.auth.jwt-current-secret")) {
            return;
        }
        if (!isTestProfile(environment)) {
            return;
        }

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("app.auth.jwt-current-secret", TestJwtSecrets.currentSecret());
        environment.getPropertySources().addFirst(new MapPropertySource("testJwtSecrets", properties));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private boolean isTestProfile(ConfigurableEnvironment environment) {
        for (String profile : environment.getActiveProfiles()) {
            if ("test".equals(profile) || "api-tests".equals(profile)) {
                return true;
            }
        }

        String configuredProfiles = environment.getProperty("spring.profiles.active", "");
        return configuredProfiles.contains("test") || configuredProfiles.contains("api-tests");
    }
}
