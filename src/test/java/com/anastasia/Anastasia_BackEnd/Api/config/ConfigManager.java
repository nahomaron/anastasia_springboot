package com.anastasia.Anastasia_BackEnd.Api.config;

import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.util.Properties;

/**
 * Utility class for managing environment-specific test configuration.
 * Picks the appropriate root-level {@code application-<env>.yml} file from {@code src/test/resources}.
 */
public class ConfigManager {

    private static final Properties props = new Properties();
    private static final String environment;

    static {
        environment = resolveEnvironment();
        String fileName = "application-" + environment + ".yml";

        YamlPropertiesFactoryBean yamlFactory = new YamlPropertiesFactoryBean();
        yamlFactory.setResources(new ClassPathResource(fileName));

        Properties loaded = yamlFactory.getObject();
        if (loaded == null) {
            throw new RuntimeException(fileName + " not found in test resources");
        }
        props.putAll(loaded);
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
