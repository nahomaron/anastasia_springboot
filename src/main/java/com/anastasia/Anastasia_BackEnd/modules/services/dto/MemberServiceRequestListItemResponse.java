package com.anastasia.Anastasia_BackEnd.modules.services.dto;

import com.anastasia.Anastasia_BackEnd.modules.services.model.BaptismRequestStatus;

import java.time.Instant;

public record MemberServiceRequestListItemResponse(
        Long id,
        String requestNumber,
        String serviceType,
        BaptismRequestStatus status,
        Instant submittedAt,
        Instant reviewedAt,
        String requestedForName,
        String churchName,
        String churchNumber
) {
}
