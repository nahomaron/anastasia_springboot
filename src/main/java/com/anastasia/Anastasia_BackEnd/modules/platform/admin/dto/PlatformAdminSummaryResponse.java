package com.anastasia.Anastasia_BackEnd.modules.platform.admin.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class PlatformAdminSummaryResponse {
    private long totalTenants;
    private long activeTenants;
    private long suspendedTenants;
    private long pendingApprovals;
    private long activeMembers;
    private long monthlyRevenue;
    private long openSupportTickets;
    private long unresolvedIncidents;
    private Instant latestHeartbeat;
}
