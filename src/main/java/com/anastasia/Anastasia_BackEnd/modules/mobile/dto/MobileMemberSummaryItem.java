package com.anastasia.Anastasia_BackEnd.modules.mobile.dto;

import java.time.Instant;

public record MobileMemberSummaryItem(
        Long id,
        String membershipNumber,
        String displayName,
        String avatarUrl,
        String status,
        String phone,
        String email,
        String churchNumber,
        Instant createdAt
) {
}
