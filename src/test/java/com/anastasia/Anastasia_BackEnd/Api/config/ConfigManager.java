package com.anastasia.Anastasia_BackEnd.Api.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Utility class for managing environment-specific configuration properties.
 * Picks the appropriate file from {@code src/test/resources/config} based on the active environment.
 */
public class ConfigManager {

    private static final Properties props = new Properties();
    private static final String environment;

    static {
        environment = resolveEnvironment();
        String fileName = "config/" + environment + ".properties";
        try (InputStream input = ConfigManager.class.getClassLoader().getResourceAsStream(fileName)) {
            if (input == null) {
                throw new RuntimeException(environment + ".properties not found in resources folder");
            }
            props.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load " + environment + ".properties", e);
        }
    }

    public static String get(String key) {
        return props.getProperty(key);
    }

    public static String getEnvironment() {
        return environment;
    }

    private static String resolveEnvironment() {
        String directProperty = System.getProperty("environment");
        if (hasText(directProperty)) {
            return directProperty.trim();
        }

        String springProfiles = System.getProperty("spring.profiles.active");
        if (hasText(springProfiles)) {
            return springProfiles.split(",")[0].trim();
        }

        String envVar = System.getenv("ENVIRONMENT");
        if (hasText(envVar)) {
            return envVar.trim();
        }

        String springEnvVar = System.getenv("SPRING_PROFILES_ACTIVE");
        if (hasText(springEnvVar)) {
            return springEnvVar.split(",")[0].trim();
        }

        return "dev";
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
