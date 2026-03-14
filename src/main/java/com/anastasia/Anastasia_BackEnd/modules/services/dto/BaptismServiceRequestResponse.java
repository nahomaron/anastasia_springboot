package com.anastasia.Anastasia_BackEnd.modules.services.dto;

import com.anastasia.Anastasia_BackEnd.modules.services.model.BaptismRequestStatus;

import java.time.Instant;

public record BaptismServiceRequestResponse(
        Long id,
        String requestNumber,
        BaptismRequestStatus status,
        Instant submittedAt
) {
}
