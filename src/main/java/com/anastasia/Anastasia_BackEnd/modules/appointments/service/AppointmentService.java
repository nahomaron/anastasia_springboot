package com.anastasia.Anastasia_BackEnd.modules.appointments.service;

import com.anastasia.Anastasia_BackEnd.modules.appointments.dto.AppointmentAssigneeRequest;
import com.anastasia.Anastasia_BackEnd.modules.appointments.dto.AppointmentCreateRequest;
import com.anastasia.Anastasia_BackEnd.modules.appointments.dto.AppointmentParticipantRequest;
import com.anastasia.Anastasia_BackEnd.modules.appointments.dto.AppointmentRescheduleRequest;
import com.anastasia.Anastasia_BackEnd.modules.appointments.dto.AppointmentResponse;
import com.anastasia.Anastasia_BackEnd.modules.appointments.dto.AppointmentStatusUpdateRequest;
import com.anastasia.Anastasia_BackEnd.modules.appointments.model.AppointmentStatus;
import com.anastasia.Anastasia_BackEnd.modules.appointments.model.AppointmentType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AppointmentService {

    AppointmentResponse createAppointment(AppointmentCreateRequest request, UUID userId);

    AppointmentResponse getAppointment(UUID appointmentId);

    List<AppointmentResponse> listAppointments(Instant start, Instant end, AppointmentStatus status, AppointmentType type);

    AppointmentResponse rescheduleAppointment(UUID appointmentId, AppointmentRescheduleRequest request, UUID userId);

    AppointmentResponse updateStatus(UUID appointmentId, AppointmentStatusUpdateRequest request, UUID userId);

    AppointmentResponse addAssignees(UUID appointmentId, List<AppointmentAssigneeRequest> assignees, UUID userId);

    AppointmentResponse removeAssignee(UUID appointmentId, UUID userIdToRemove);

    AppointmentResponse addParticipants(UUID appointmentId, List<AppointmentParticipantRequest> participants);

    AppointmentResponse removeParticipant(UUID appointmentId, UUID participantId);
}
