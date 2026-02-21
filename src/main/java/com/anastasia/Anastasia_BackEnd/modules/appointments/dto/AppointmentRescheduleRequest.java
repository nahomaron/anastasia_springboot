package com.anastasia.Anastasia_BackEnd.modules.appointments.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record AppointmentRescheduleRequest(
        @NotNull Instant newStart,
        Instant newEnd,
        String reason
) {}
