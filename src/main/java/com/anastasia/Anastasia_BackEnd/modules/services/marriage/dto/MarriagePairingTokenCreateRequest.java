package com.anastasia.Anastasia_BackEnd.modules.services.marriage.dto;

import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriagePartyRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

public record MarriagePairingTokenCreateRequest(
        @NotNull MarriagePartyRole partyRole,
        @Email String inviteEmail,
        Integer expiresInDays
) {
}
