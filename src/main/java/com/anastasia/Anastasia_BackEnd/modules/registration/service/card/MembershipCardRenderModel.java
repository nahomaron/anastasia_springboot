package com.anastasia.Anastasia_BackEnd.modules.registration.service.card;

import java.time.LocalDate;

public record MembershipCardRenderModel(
        String memberFullName,
        LocalDate dateOfBirth,
        String churchName,
        LocalDate issueDate,
        LocalDate expirationDate,
        String membershipNumber,
        String cardSerialNumber,
        String qrPayload,
        String memberAvatarUrl,
        String churchLogoUrl,
        String templateDisplayName,
        String primaryColor,
        String accentColor,
        String textColor
) {
}
