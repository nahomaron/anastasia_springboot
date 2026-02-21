package com.anastasia.Anastasia_BackEnd.modules.appointments.dto;

import com.anastasia.Anastasia_BackEnd.modules.appointments.model.AssignedRole;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AppointmentAssigneeRequest(
        @NotNull UUID userId,
        @NotNull AssignedRole role
) {}
