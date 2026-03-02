package com.anastasia.Anastasia_BackEnd.modules.users.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class MemberTransferCreateRequest {

    @NotNull
    private UUID targetTenantId;

    private String reason;
}
