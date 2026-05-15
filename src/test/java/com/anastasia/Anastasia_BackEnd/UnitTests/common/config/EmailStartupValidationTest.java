package com.anastasia.Anastasia_BackEnd.UnitTests.common.config;

import com.anastasia.Anastasia_BackEnd.common.config.EmailStartupValidation;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailStartupValidationTest {

    @Test
    void failsFastWhenEmailIsEnabledButMailConfigIsMissing() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("notification.email.enabled", "true")
                .withProperty("spring.mail.host", "email-smtp.us-east-2.amazonaws.com")
                .withProperty("app.public.frontend-base-url", "https://app.anastasia.com");
        environment.setActiveProfiles("dev");

        EmailStartupValidation validation = new EmailStartupValidation(environment, true);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> validation.validate());
        assertTrue(exception.getMessage().contains("spring.mail.username"));
        assertTrue(exception.getMessage().contains("spring.mail.password"));
        assertTrue(exception.getMessage().contains("spring.mail.from"));
    }

    @Test
    void allowsStartupWhenRequiredMailConfigIsPresent() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("notification.email.enabled", "true")
                .withProperty("spring.mail.host", "email-smtp.us-east-2.amazonaws.com")
                .withProperty("spring.mail.username", "smtp-user")
                .withProperty("spring.mail.password", "smtp-pass")
                .withProperty("spring.mail.from", "noreply@anastasisapp.com")
                .withProperty("app.public.frontend-base-url", "https://app.anastasia.com");
        environment.setActiveProfiles("dev");

        EmailStartupValidation validation = new EmailStartupValidation(environment, true);

        assertDoesNotThrow(() -> validation.validate());
    }

    @Test
    void skipsValidationForTestProfiles() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("notification.email.enabled", "true");
        environment.setActiveProfiles("test");

        EmailStartupValidation validation = new EmailStartupValidation(environment, true);

        assertDoesNotThrow(() -> validation.validate());
    }
}
