package com.anastasia.Anastasia_BackEnd.core.auth.service;

import java.time.Instant;

public record IssuedPasswordResetToken(
        int tokenId,
        String rawToken,
        Instant expiresAt
) {
}
