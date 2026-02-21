package com.anastasia.Anastasia_BackEnd.modules.appointments.dto;

import com.anastasia.Anastasia_BackEnd.modules.appointments.model.ParticipantRole;

import java.util.UUID;

public record AppointmentParticipantResponse(
        UUID id,
        Long memberId,
        String fullName,
        boolean familyMember,
        ParticipantRole role
) {}
