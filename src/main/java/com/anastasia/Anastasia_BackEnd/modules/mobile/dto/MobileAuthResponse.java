package com.anastasia.Anastasia_BackEnd.modules.mobile.dto;

import java.util.Set;
import java.util.UUID;

public record MobileAuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        Long expiresIn,
        MobileUserResponse user,
        MobileTenantResponse tenant,
        MobileSessionResponse session
) {
}
