package com.anastasia.Anastasia_BackEnd.modules.appointments.dto;

import com.anastasia.Anastasia_BackEnd.modules.appointments.model.AppointmentSource;
import com.anastasia.Anastasia_BackEnd.modules.appointments.model.AppointmentStatus;
import com.anastasia.Anastasia_BackEnd.modules.appointments.model.AppointmentType;
import com.anastasia.Anastasia_BackEnd.modules.appointments.model.LocationType;
import com.anastasia.Anastasia_BackEnd.modules.calendar.model.CalendarVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record AppointmentCreateRequest(
        @NotBlank String title,
        String description,
        @NotNull AppointmentType type,
        @NotNull Instant startDateTime,
        Instant endDateTime,
        @NotBlank String timeZone,
        @NotNull LocationType locationType,
        @NotBlank String locationLabel,
        AppointmentStatus status,
        AppointmentSource source,
        String notesForMember,
        String contactInfo,
        UUID linkedRequestId,
        CalendarVisibility visibility,
        Set<AppointmentParticipantRequest> participants,
        Set<AppointmentAssigneeRequest> assignees
) {}
