package com.anastasia.Anastasia_BackEnd.modules.appointments.dto;

import com.anastasia.Anastasia_BackEnd.modules.appointments.model.AssignedRole;

import java.util.UUID;

public record AppointmentAssigneeResponse(
        UUID userId,
        String fullName,
        AssignedRole role
) {}
