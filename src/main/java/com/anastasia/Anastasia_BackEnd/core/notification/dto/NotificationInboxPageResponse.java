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

    @Override
    public List<NotificationInboxItemResponse> items() {
        return copyList(items);
    }

    @Override
    public List<Integer> sizeOptions() {
        return copyList(sizeOptions);
    }

    private static <T> List<T> copyList(List<T> input) {
        return input == null ? List.of() : List.copyOf(input);
    }
}
