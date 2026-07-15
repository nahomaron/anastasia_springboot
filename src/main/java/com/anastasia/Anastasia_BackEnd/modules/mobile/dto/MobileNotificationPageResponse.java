package com.anastasia.Anastasia_BackEnd.modules.mobile.dto;

import java.util.List;

public record MobileNotificationPageResponse(
        List<MobileNotificationItem> items,
        int page,
        int size,
        int totalPages,
        long totalElements,
        long unreadCount
) {
}
