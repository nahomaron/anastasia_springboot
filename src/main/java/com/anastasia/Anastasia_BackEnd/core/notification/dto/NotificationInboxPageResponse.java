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

    public NotificationInboxPageResponse(
            List<NotificationInboxItemResponse> items,
            int page,
            int size,
            int totalPages,
            long totalElements,
            List<Integer> sizeOptions,
            long unreadCount
    ) {
        this.items = copyList(items);
        this.sizeOptions = copyList(sizeOptions);
        this.page = page;
        this.size = size;
        this.totalPages = totalPages;
        this.totalElements = totalElements;
        this.unreadCount = unreadCount;
    }

    private static <T> List<T> copyList(List<T> input) {
        return input == null ? List.of() : List.copyOf(input);
    }
}
