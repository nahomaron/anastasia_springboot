package com.anastasia.Anastasia_BackEnd.modules.registration.dto.card;

import lombok.Builder;

import java.time.LocalDate;

@Builder
public record MembershipCardVerifyResponse(
        boolean valid,
        String churchName,
        String diocese,
        LocalDate expirationDate,
        String maskedMemberLabel
) {
}
