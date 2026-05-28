package com.anastasia.Anastasia_BackEnd.UnitTests.common.config;

import com.anastasia.Anastasia_BackEnd.common.config.PublicUrlStartupValidation;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublicUrlStartupValidationTest {

    @Test
    void allowsHttpUrlsForLocalProfiles() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("app.public.frontend-base-url", "http://localhost:4200")
                .withProperty("app.public.backend-base-url", "http://localhost:8080")
                .withProperty("app.cors.allowed-origins", "http://localhost:4200,http://127.0.0.1:4200");
        environment.setActiveProfiles("dev");

        PublicUrlStartupValidation validation = new PublicUrlStartupValidation(environment);
        ReflectionTestUtils.setField(validation, "frontendBaseUrl", "http://localhost:4200");
        ReflectionTestUtils.setField(validation, "backendBaseUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(validation, "allowedOrigins", "http://localhost:4200,http://127.0.0.1:4200");

        assertDoesNotThrow(validation::validate);
    }

    @Test
    void rejectsNonHttpsUrlsOutsideLocalProfiles() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        PublicUrlStartupValidation validation = new PublicUrlStartupValidation(environment);
        ReflectionTestUtils.setField(validation, "frontendBaseUrl", "http://app.anastasisapp.com");
        ReflectionTestUtils.setField(validation, "backendBaseUrl", "https://api.anastasisapp.com");
        ReflectionTestUtils.setField(validation, "allowedOrigins", "https://app.anastasisapp.com,http://admin.anastasisapp.com");

        IllegalStateException exception = assertThrows(IllegalStateException.class, validation::validate);
        assertTrue(exception.getMessage().contains("app.public.frontend-base-url must use https"));
        assertTrue(exception.getMessage().contains("app.cors.allowed-origins contains non-https origin"));
    }

    @Test
    void acceptsHttpsUrlsOutsideLocalProfiles() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        PublicUrlStartupValidation validation = new PublicUrlStartupValidation(environment);
        ReflectionTestUtils.setField(validation, "frontendBaseUrl", "https://app.anastasisapp.com");
        ReflectionTestUtils.setField(validation, "backendBaseUrl", "https://api.anastasisapp.com");
        ReflectionTestUtils.setField(validation, "allowedOrigins", "https://app.anastasisapp.com,https://admin.anastasisapp.com");

        assertDoesNotThrow(validation::validate);
    }
}
