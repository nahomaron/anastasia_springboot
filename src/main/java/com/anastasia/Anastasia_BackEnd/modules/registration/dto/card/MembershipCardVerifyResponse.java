package com.anastasia.Anastasia_BackEnd.modules.registration.dto.card;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.card.MembershipCardStatus;
import lombok.Builder;

import java.time.LocalDate;

@Builder
public record MembershipCardVerifyResponse(
        boolean valid,
        String message,
        String memberFullName,
        String churchName,
        String membershipNumber,
        LocalDate issueDate,
        LocalDate expirationDate,
        MembershipCardStatus status,
        String cardSerialNumber
) {
}
