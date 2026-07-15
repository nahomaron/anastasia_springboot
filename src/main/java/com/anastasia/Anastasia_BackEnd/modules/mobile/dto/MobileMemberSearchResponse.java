package com.anastasia.Anastasia_BackEnd.modules.mobile.dto;

import java.util.List;

public record MobileMemberSearchResponse(
        List<MobileMemberSummaryItem> items,
        int page,
        int size,
        int totalPages,
        long totalElements,
        boolean hasNext
) {
}
