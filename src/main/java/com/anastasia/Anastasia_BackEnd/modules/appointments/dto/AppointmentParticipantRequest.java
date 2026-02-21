package com.anastasia.Anastasia_BackEnd.modules.appointments.dto;

import com.anastasia.Anastasia_BackEnd.modules.appointments.model.ParticipantRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AppointmentParticipantRequest(
        Long memberId,
        @NotBlank String fullName,
        boolean familyMember,
        @NotNull ParticipantRole role
) {}
