package com.anastasia.Anastasia_BackEnd.modules.mobile.dto;

import java.util.Set;
import java.util.UUID;

public record MobileUserResponse(
        UUID id,
        String fullName,
        String email,
        String phone,
        Set<String> roles,
        Set<String> permissions
) {
}
