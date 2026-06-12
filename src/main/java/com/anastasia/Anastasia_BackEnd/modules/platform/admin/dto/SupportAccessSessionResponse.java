package com.anastasia.Anastasia_BackEnd.modules.platform.admin.dto;

import com.anastasia.Anastasia_BackEnd.modules.platform.admin.model.SupportAccessScope;
import com.anastasia.Anastasia_BackEnd.modules.platform.admin.model.SupportAccessSessionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportAccessSessionResponse {
    private UUID sessionId;
    private UUID actorUserId;
    private String actorName;
    private String actorEmail;
    private UUID tenantId;
    private String tenantName;
    private String reason;
    private SupportAccessScope scope;
    private SupportAccessSessionStatus status;
    private String denialReason;
    private String endReason;
    private Instant startedAt;
    private Instant endedAt;
    private Instant lastActivityAt;
    private Instant createdAt;
    private List<SupportAccessActionResponse> actions;
}
