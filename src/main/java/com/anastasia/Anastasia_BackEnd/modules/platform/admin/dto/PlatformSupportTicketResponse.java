package com.anastasia.Anastasia_BackEnd.modules.platform.admin.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class PlatformSupportTicketResponse {
    private String ticketId;
    private String subject;
    private String priority;
    private String status;
    private String tenantName;
    private String requestedBy;
    private Instant openedAt;
    private String channel;
}
