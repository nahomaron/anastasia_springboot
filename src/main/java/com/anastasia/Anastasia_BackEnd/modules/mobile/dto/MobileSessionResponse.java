package com.anastasia.Anastasia_BackEnd.modules.mobile.dto;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record MobileSessionResponse(
        UUID userId,
        String email,
        String fullName,
        Set<String> roles,
        Set<String> permissions,
        boolean authenticated,
        boolean requiresPasswordChange,
        Instant expiresAt
) {
}
