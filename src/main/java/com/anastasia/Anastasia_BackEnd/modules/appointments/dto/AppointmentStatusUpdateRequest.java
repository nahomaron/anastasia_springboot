package com.anastasia.Anastasia_BackEnd.modules.appointments.dto;

import com.anastasia.Anastasia_BackEnd.modules.appointments.model.AppointmentStatus;
import jakarta.validation.constraints.NotNull;

public record AppointmentStatusUpdateRequest(
        @NotNull AppointmentStatus status,
        String reason
) {}
