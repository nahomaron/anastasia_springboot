package com.anastasia.Anastasia_BackEnd.modules.mobile.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record MobileCheckInRequest(
        @NotNull Long eventId,
        UUID memberId,
        String guestFullName,
        String guestEmail,
        String guestPhone,
        String checkInMethod
) {
}
