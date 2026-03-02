package com.anastasia.Anastasia_BackEnd.modules.appointments.controller;

import com.anastasia.Anastasia_BackEnd.core.auth.principal.UserPrincipal;
import com.anastasia.Anastasia_BackEnd.modules.appointments.dto.AppointmentAssigneeRequest;
import com.anastasia.Anastasia_BackEnd.modules.appointments.dto.AppointmentCreateRequest;
import com.anastasia.Anastasia_BackEnd.modules.appointments.dto.AppointmentParticipantRequest;
import com.anastasia.Anastasia_BackEnd.modules.appointments.dto.AppointmentRescheduleRequest;
import com.anastasia.Anastasia_BackEnd.modules.appointments.dto.AppointmentResponse;
import com.anastasia.Anastasia_BackEnd.modules.appointments.dto.AppointmentStatusUpdateRequest;
import com.anastasia.Anastasia_BackEnd.modules.appointments.model.AppointmentStatus;
import com.anastasia.Anastasia_BackEnd.modules.appointments.model.AppointmentType;
import com.anastasia.Anastasia_BackEnd.modules.appointments.service.AppointmentService;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantFeature;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.entitlement.RequiresTenantFeature;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/appointments")
@RequiresTenantFeature(TenantFeature.APPOINTMENTS)
public class AppointmentController {

    private final AppointmentService appointmentService;

    @PreAuthorize("hasAnyRole('OWNER', 'PRIEST') or @permissionEvaluator.hasAny(authentication, 'MANAGE_APPOINTMENT', 'BOOK_APPOINTMENT')")
    @PostMapping
    public ResponseEntity<AppointmentResponse> createAppointment(@Valid @RequestBody AppointmentCreateRequest request) {
        UUID userId = resolveCurrentUserId();
        AppointmentResponse response = appointmentService.createAppointment(request, userId);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('OWNER', 'PRIEST') or @permissionEvaluator.hasAny(authentication, 'MANAGE_APPOINTMENT', 'BOOK_APPOINTMENT')")
    @GetMapping
    public ResponseEntity<List<AppointmentResponse>> listAppointments(
            @RequestParam(value = "start", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @RequestParam(value = "end", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end,
            @RequestParam(value = "status", required = false) AppointmentStatus status,
            @RequestParam(value = "type", required = false) AppointmentType type
    ) {
        List<AppointmentResponse> responses = appointmentService.listAppointments(start, end, status, type);
        return ResponseEntity.ok(responses);
    }

    @PreAuthorize("hasAnyRole('OWNER', 'PRIEST') or @permissionEvaluator.hasAny(authentication, 'MANAGE_APPOINTMENT', 'BOOK_APPOINTMENT')")
    @GetMapping("/{appointmentId}")
    public ResponseEntity<AppointmentResponse> getAppointment(@PathVariable UUID appointmentId) {
        return ResponseEntity.ok(appointmentService.getAppointment(appointmentId));
    }

    @PreAuthorize("hasAnyRole('OWNER', 'PRIEST') or @permissionEvaluator.hasAny(authentication, 'MANAGE_APPOINTMENT', 'BOOK_APPOINTMENT')")
    @PatchMapping("/{appointmentId}/reschedule")
    public ResponseEntity<AppointmentResponse> rescheduleAppointment(
            @PathVariable UUID appointmentId,
            @Valid @RequestBody AppointmentRescheduleRequest request
    ) {
        UUID userId = resolveCurrentUserId();
        return ResponseEntity.ok(appointmentService.rescheduleAppointment(appointmentId, request, userId));
    }

    @PreAuthorize("hasAnyRole('OWNER', 'PRIEST') or @permissionEvaluator.hasAny(authentication, 'MANAGE_APPOINTMENT', 'CANCEL_APPOINTMENT')")
    @PatchMapping("/{appointmentId}/status")
    public ResponseEntity<AppointmentResponse> updateStatus(
            @PathVariable UUID appointmentId,
            @Valid @RequestBody AppointmentStatusUpdateRequest request
    ) {
        UUID userId = resolveCurrentUserId();
        return ResponseEntity.ok(appointmentService.updateStatus(appointmentId, request, userId));
    }

    @PreAuthorize("hasAnyRole('OWNER', 'PRIEST') or @permissionEvaluator.hasAny(authentication, 'MANAGE_APPOINTMENT')")
    @PostMapping("/{appointmentId}/assignees")
    public ResponseEntity<AppointmentResponse> addAssignees(
            @PathVariable UUID appointmentId,
            @Valid @RequestBody List<AppointmentAssigneeRequest> assignees
    ) {
        UUID userId = resolveCurrentUserId();
        return ResponseEntity.ok(appointmentService.addAssignees(appointmentId, assignees, userId));
    }

    @PreAuthorize("hasAnyRole('OWNER', 'PRIEST') or @permissionEvaluator.hasAny(authentication, 'MANAGE_APPOINTMENT')")
    @DeleteMapping("/{appointmentId}/assignees/{userId}")
    public ResponseEntity<AppointmentResponse> removeAssignee(
            @PathVariable UUID appointmentId,
            @PathVariable UUID userId
    ) {
        return ResponseEntity.ok(appointmentService.removeAssignee(appointmentId, userId));
    }

    @PreAuthorize("hasAnyRole('OWNER', 'PRIEST') or @permissionEvaluator.hasAny(authentication, 'MANAGE_APPOINTMENT')")
    @PostMapping("/{appointmentId}/participants")
    public ResponseEntity<AppointmentResponse> addParticipants(
            @PathVariable UUID appointmentId,
            @Valid @RequestBody List<AppointmentParticipantRequest> participants
    ) {
        return ResponseEntity.ok(appointmentService.addParticipants(appointmentId, participants));
    }

    @PreAuthorize("hasAnyRole('OWNER', 'PRIEST') or @permissionEvaluator.hasAny(authentication, 'MANAGE_APPOINTMENT')")
    @DeleteMapping("/{appointmentId}/participants/{participantId}")
    public ResponseEntity<AppointmentResponse> removeParticipant(
            @PathVariable UUID appointmentId,
            @PathVariable UUID participantId
    ) {
        return ResponseEntity.ok(appointmentService.removeParticipant(appointmentId, participantId));
    }

    private UUID resolveCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal userPrincipal) {
            return userPrincipal.getUserUuid();
        }
        throw new IllegalStateException("Unable to resolve the authenticated user");
    }
}
