package com.anastasia.Anastasia_BackEnd.modules.mobile.dto;

import java.util.List;
import java.util.UUID;

public record MobileDashboardSummaryResponse(
        String role,
        UUID tenantId,
        String tenantName,
        List<MobileStatItem> stats,
        List<MobileActionItem> quickActions,
        MobileDashboardHighlights highlights
) {
}
