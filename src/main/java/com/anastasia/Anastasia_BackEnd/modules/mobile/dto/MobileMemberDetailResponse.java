package com.anastasia.Anastasia_BackEnd.modules.mobile.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public record MobileMemberDetailResponse(
        Long id,
        UUID tenantId,
        String membershipNumber,
        String churchNumber,
        String displayName,
        String fullNameLocal,
        String status,
        String avatarUrl,
        String phone,
        String email,
        String gender,
        LocalDate birthday,
        Map<String, Object> address,
        Map<String, Object> family,
        Map<String, Object> membership,
        Map<String, Boolean> actions
) {
}
