package com.anastasia.Anastasia_BackEnd.modules.mobile.dto;

import java.util.List;

public record MobileDashboardHighlights(
        List<MobileMemberSummaryItem> members,
        List<MobileEventSummaryItem> events,
        List<MobileNotificationItem> notifications
) {
}
