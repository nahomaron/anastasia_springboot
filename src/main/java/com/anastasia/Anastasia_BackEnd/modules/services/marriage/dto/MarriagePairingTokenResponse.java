package com.anastasia.Anastasia_BackEnd.modules.services.marriage.dto;

import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriagePartyRole;

import java.time.Instant;
import java.util.UUID;

public record MarriagePairingTokenResponse(
        UUID id,
        UUID marriageCaseId,
        MarriagePartyRole partyRole,
        String token,
        String inviteEmail,
        Instant expiresAt,
        boolean active
) {
}
