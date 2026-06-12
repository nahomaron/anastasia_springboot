package com.anastasia.Anastasia_BackEnd.modules.platform.admin.dto;

import com.anastasia.Anastasia_BackEnd.modules.platform.admin.model.SupportAccessActionOutcome;
import com.anastasia.Anastasia_BackEnd.modules.platform.admin.model.SupportAccessActionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportAccessActionResponse {
    private UUID actionId;
    private SupportAccessActionType actionType;
    private String httpMethod;
    private String requestPath;
    private int responseStatus;
    private SupportAccessActionOutcome outcome;
    private String detail;
    private Instant occurredAt;
}
