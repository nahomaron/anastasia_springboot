package com.anastasia.Anastasia_BackEnd.modules.platform.admin.service;

import java.time.Instant;
import java.util.UUID;

public record PlatformAdminRecoveryTokenResult(
        String email,
        UUID userId,
        String resetUrl,
        Instant expiresAt
) {
}
