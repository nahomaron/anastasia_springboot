package com.anastasia.Anastasia_BackEnd.modules.appointments.dto;

import com.anastasia.Anastasia_BackEnd.modules.appointments.model.AppointmentStatus;

import java.time.Instant;
import java.util.UUID;

public record AppointmentStatusHistoryResponse(
        AppointmentStatus fromStatus,
        AppointmentStatus toStatus,
        String reason,
        UUID changedByUserId,
        Instant changedAt
) {}
