package com.anastasia.Anastasia_BackEnd.modules.registration.service.onboarding;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WebhookReceiptSanitizerTest {

    @Test
    void summarizePayload_replacesRawPayloadWithFingerprint() {
        String payload = "{\"id\":\"evt_123\",\"email\":\"user@example.com\"}";

        String summary = WebhookReceiptSanitizer.summarizePayload(payload);

        assertNotNull(summary);
        assertTrue(summary.contains("\"payloadSize\":"));
        assertTrue(summary.contains("\"payloadSha256\":\""));
        assertFalse(summary.contains("user@example.com"));
        assertFalse(summary.contains("evt_123"));
    }

    @Test
    void summarizePayload_returnsNullForBlankPayload() {
        assertNull(WebhookReceiptSanitizer.summarizePayload(" "));
    }
}
