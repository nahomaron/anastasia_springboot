package com.anastasia.Anastasia_BackEnd.modules.mobile.dto;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantType;

import java.util.UUID;

public record MobileTenantResponse(
        UUID id,
        String displayName,
        String slug,
        TenantStatus status,
        TenantType tenantType,
        boolean phoneVerified,
        String defaultTimezone,
        String defaultLocale,
        String countryCode
) {
}
