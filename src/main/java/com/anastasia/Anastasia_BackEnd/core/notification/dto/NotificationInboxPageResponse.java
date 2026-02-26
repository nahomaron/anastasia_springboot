package com.anastasia.Anastasia_BackEnd.core.notification.dto;

import java.util.List;

public record NotificationInboxPageResponse(
        List<NotificationInboxItemResponse> items,
        int page,
        int size,
        int totalPages,
        long totalElements,
        List<Integer> sizeOptions,
        long unreadCount
) {
}
