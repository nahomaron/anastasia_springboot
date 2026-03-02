package com.anastasia.Anastasia_BackEnd.modules.registration.dto.card;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.card.MembershipCardStatus;
import lombok.Builder;

import java.time.LocalDate;

@Builder
public record MembershipCardSummaryResponse(
        Long cardId,
        String membershipNumber,
        String memberFullName,
        String churchName,
        LocalDate issueDate,
        LocalDate expirationDate,
        String cardSerialNumber,
        MembershipCardStatus status,
        String templateKey,
        String templateDisplayName,
        String memberAvatarUrl,
        String churchLogoUrl
) {
}
