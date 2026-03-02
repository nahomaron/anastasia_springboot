package com.anastasia.Anastasia_BackEnd.modules.users.dto;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.MemberTransferStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class MemberTransferResponse {

    private UUID id;
    private UUID userId;
    private UUID fromTenantId;
    private UUID toTenantId;
    private MemberTransferStatus status;
    private String reason;
    private String decisionNote;
    private UUID requestedByUserId;
    private UUID decidedByUserId;
    private LocalDateTime requestedAt;
    private LocalDateTime decidedAt;
    private LocalDateTime executedAt;
}
