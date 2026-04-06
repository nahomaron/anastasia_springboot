package com.anastasia.Anastasia_BackEnd.modules.appointments.dto;

import com.anastasia.Anastasia_BackEnd.modules.appointments.model.AppointmentSource;
import com.anastasia.Anastasia_BackEnd.modules.appointments.model.AppointmentStatus;
import com.anastasia.Anastasia_BackEnd.modules.appointments.model.AppointmentType;
import com.anastasia.Anastasia_BackEnd.modules.appointments.model.ContactPreference;
import com.anastasia.Anastasia_BackEnd.modules.appointments.model.LocationType;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record AppointmentResponse(
        UUID id,
        String title,
        String description,
        AppointmentType type,
        Instant startDateTime,
        Instant endDateTime,
        String timeZone,
        LocationType locationType,
        String locationLabel,
        AppointmentStatus status,
        AppointmentSource source,
        AppointmentParticipantResponse member,
        AppointmentAssigneeResponse primaryAssignee,
        Set<AppointmentParticipantResponse> participants,
        Set<AppointmentAssigneeResponse> assignees,
        UUID linkedRequestId,
        String notesForMember,
        String contactPhone,
        String contactEmail,
        ContactPreference contactPreference,
        Boolean privateNotesExists,
        Boolean firstVisit,
        Boolean sacramentRelated,
        Instant requestedAt,
        Instant confirmedAt,
        Instant completedAt,
        Instant canceledAt,
        String cancellationReason,
        String outcomeNotes,
        Set<AppointmentStatusHistoryResponse> statusHistory
 ) {

    public AppointmentResponse {
        participants = copySet(participants);
        assignees = copySet(assignees);
        statusHistory = copySet(statusHistory);
    }

    private static <T> Set<T> copySet(Set<T> input) {
        return input == null ? Set.of() : Set.copyOf(input);
    }
}
