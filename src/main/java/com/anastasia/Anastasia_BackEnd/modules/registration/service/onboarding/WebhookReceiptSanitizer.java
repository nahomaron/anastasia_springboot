package com.anastasia.Anastasia_BackEnd.modules.registration.service.onboarding;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

final class WebhookReceiptSanitizer {

    private WebhookReceiptSanitizer() {
    }

    static String summarizePayload(String payload) {
        if (payload == null || payload.isBlank()) {
            return null;
        }
        return "{\"payloadSize\":%d,\"payloadSha256\":\"%s\"}".formatted(
                payload.length(),
                sha256(payload)
        );
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 digest unavailable", ex);
        }
    }
}
