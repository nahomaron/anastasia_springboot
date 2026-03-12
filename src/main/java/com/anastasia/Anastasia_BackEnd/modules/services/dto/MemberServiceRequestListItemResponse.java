package com.anastasia.Anastasia_BackEnd.modules.services.dto;

import com.anastasia.Anastasia_BackEnd.modules.services.model.BaptismRequestStatus;

import java.time.LocalDateTime;

public record MemberServiceRequestListItemResponse(
        Long id,
        String requestNumber,
        String serviceType,
        BaptismRequestStatus status,
        LocalDateTime submittedAt,
        LocalDateTime reviewedAt,
        String requestedForName,
        String churchName,
        String churchNumber
) {
}
