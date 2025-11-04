package com.anastasia.Anastasia_BackEnd.modules.registration.model.member;

import java.time.Instant;
import java.util.UUID;

public record RegistrationCompletedEvent(
        UUID tenantId,
        Long memberId,
        String memberEmail,
        String memberName,
        Instant registeredAt
) {}
