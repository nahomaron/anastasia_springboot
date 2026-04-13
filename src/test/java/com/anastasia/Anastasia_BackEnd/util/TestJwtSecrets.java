package com.anastasia.Anastasia_BackEnd.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class TestJwtSecrets {
    private static final byte[] CURRENT_SECRET_BYTES =
            "anastasia-test-current-jwt-material-01".getBytes(StandardCharsets.UTF_8);
    private static final byte[] PREVIOUS_SECRET_BYTES =
            "anastasia-test-previous-jwt-material-02".getBytes(StandardCharsets.UTF_8);

    private TestJwtSecrets() {
    }

    public static String currentSecret() {
        return Base64.getEncoder().encodeToString(CURRENT_SECRET_BYTES);
    }

    public static String previousSecret() {
        return Base64.getEncoder().encodeToString(PREVIOUS_SECRET_BYTES);
    }
}
