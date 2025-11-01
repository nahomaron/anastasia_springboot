package com.anastasia.Anastasia_BackEnd.modules.events.controller;

import com.anastasia.Anastasia_BackEnd.modules.events.model.EventDTO;
import com.anastasia.Anastasia_BackEnd.modules.events.model.EventEntity;
import com.anastasia.Anastasia_BackEnd.modules.events.model.EventManagerEntity;
import com.anastasia.Anastasia_BackEnd.modules.events.model.attendance.*;
import com.anastasia.Anastasia_BackEnd.modules.events.model.requests.AssignEventManagerRequest;
import com.anastasia.Anastasia_BackEnd.modules.events.model.requests.EventManagerDTO;
import com.anastasia.Anastasia_BackEnd.core.auth.principal.UserPrincipal;
import com.anastasia.Anastasia_BackEnd.modules.events.service.EventAttendanceService;
import com.anastasia.Anastasia_BackEnd.modules.events.service.EventService;
import com.anastasia.Anastasia_BackEnd.modules.events.service.QrCheckInService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/events")
public class EventController {

    private final EventService eventService;
    private final EventAttendanceService attendanceService;
    private final QrCheckInService qrCheckInService;

    @PreAuthorize("hasAnyRole('OWNER', 'PRIEST') " +
            "or @permissionEvaluator.hasAny(authentication, 'MANAGE_EVENTS', 'VIEW_EVENTS')")
    @GetMapping("/visible")
    public ResponseEntity<List<EventDTO>> getVisibleEventsForCurrentUser() {
        UUID userId = resolveCurrentUserId();
        List<EventDTO> events = eventService.getVisibleEventsForUser(userId);
        return ResponseEntity.ok(events);
    }

    @PreAuthorize("hasAnyRole('OWNER', 'PRIEST') " +
            "or @permissionEvaluator.hasAny(authentication, 'MANAGE_EVENTS', 'CREATE_EDIT_EVENTS')")
    @PostMapping
    public ResponseEntity<?> createEvent(@RequestBody EventDTO eventDTO){
        EventEntity event = eventService.convertToEntity(eventDTO);
        EventEntity savedEvent = eventService.createEvent(event);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PreAuthorize("hasAnyRole('OWNER', 'PRIEST') " +
            "or @permissionEvaluator.hasAny(authentication, 'MANAGE_EVENTS', 'CREATE_EDIT_EVENTS')")
    @PutMapping("/{eventId}/update")
    public ResponseEntity<?> updateEvent(@PathVariable Long eventId, @RequestBody EventDTO eventDTO){
        EventEntity event = eventService.convertToEntity(eventDTO);
        EventEntity updatedEvent = eventService.updateEvent(eventId, event);
        return new ResponseEntity<>(eventService.convertToDTO(updatedEvent), HttpStatus.ACCEPTED);
    }

    @PreAuthorize("hasAnyRole('OWNER', 'PRIEST') " +
            "or @permissionEvaluator.hasAny(authentication, 'MANAGE_EVENTS', 'CREATE_EDIT_EVENTS')")
    @PostMapping("/{eventId}/managers")
    public ResponseEntity<?> assignManager(@PathVariable Long eventId,
                                           @RequestBody AssignEventManagerRequest request) {
       eventService.assignManagerToEvent(eventId, request.getUserId(), request.getRole());
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PreAuthorize("hasAnyRole('OWNER', 'PRIEST') " +
            "or @permissionEvaluator.hasAny(authentication, 'MANAGE_EVENTS', 'CREATE_EDIT_EVENTS')")
    @DeleteMapping("/{eventId}/managers/{managerId}")
    public ResponseEntity<?> removeManager(@PathVariable Long eventId, @PathVariable UUID managerId) {
        eventService.removeManager(eventId, managerId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('OWNER', 'PRIEST') " +
            "or @permissionEvaluator.hasAny(authentication, 'MANAGE_EVENTS')")
    @GetMapping("/{eventId}/managers")
    public ResponseEntity<List<EventManagerDTO>> listManagers(@PathVariable Long eventId) {
        List<EventManagerEntity> managers = eventService.getManagers(eventId);

        return new ResponseEntity<>(managers.stream()
                .map(eventService::convertToDTO)
                .toList(), HttpStatus.OK);
    }

    @PreAuthorize("hasAnyRole('OWNER', 'PRIEST') " +
            "or @permissionEvaluator.hasAny(authentication, 'MANAGE_EVENTS')")
    @DeleteMapping("/{eventId}")
    public ResponseEntity<?> deleteEvent(@PathVariable Long eventId){
        eventService.deleteEvent(eventId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PreAuthorize("hasAnyRole('OWNER', 'PRIEST') " +
            "or @permissionEvaluator.hasAny(authentication, 'MANAGE_EVENTS', 'VIEW_EVENTS')")
    @PostMapping("/event/check-in")
    public ResponseEntity<EventAttendance> checkIn(@RequestBody CheckInRequestDTO requestDTO){
        EventAttendance attendance = attendanceService.checkIn(requestDTO);
        return new ResponseEntity<>(attendance, HttpStatus.OK);
    }

    @PreAuthorize("hasAnyRole('OWNER', 'PRIEST') " +
            "or @permissionEvaluator.hasAny(authentication, 'MANAGE_EVENTS', 'VIEW_EVENTS')")
    @PostMapping("/event/check-in/qr-code")
    public ResponseEntity<EventAttendance> checkInWithQR(@RequestBody CheckInQRRequestDTO requestDTO){
        EventAttendance attendance = qrCheckInService.checkInWithQR(requestDTO);
        return new ResponseEntity<>(attendance, HttpStatus.OK);
    }


    @PreAuthorize("hasAnyRole('OWNER', 'PRIEST') " +
            "or @permissionEvaluator.hasAny(authentication, 'MANAGE_EVENTS', 'MARK_ATTENDANCE')")
    @PostMapping("/mark-absent")
    public ResponseEntity<EventAttendance> markAbsent(@RequestBody MarkAbsentRequestDTO request) {
        EventAttendance attendance = attendanceService.markAbsent(request);
        return new ResponseEntity<>(attendance, HttpStatus.OK);
    }


    @PreAuthorize("hasAnyRole('OWNER', 'PRIEST') " +
            "or @permissionEvaluator.hasAny(authentication, 'MANAGE_EVENTS', 'VIEW_EVENT_REPORTS')")
    @GetMapping("/by-event/{eventId}")
    public ResponseEntity<List<EventAttendance>> getAttendanceByEvent(@PathVariable Long eventId) {
        return ResponseEntity.ok(attendanceService.getAttendanceByEvent(eventId));
    }

    @PreAuthorize("hasAnyRole('OWNER', 'PRIEST') " +
            "or @permissionEvaluator.hasAny(authentication, 'MANAGE_EVENTS', 'VIEW_EVENT_REPORTS')")
    @GetMapping("/by-event/{eventId}/status/{status}")
    public ResponseEntity<List<EventAttendance>> getByEventAndStatus(@PathVariable Long eventId,
                                                                     @PathVariable AttendanceStatus status) {
        return ResponseEntity.ok(attendanceService.getAttendanceByEventAndStatus(eventId, status));
    }

    @PreAuthorize("hasAnyRole('OWNER', 'PRIEST') " +
            "or @permissionEvaluator.hasAny(authentication, 'MANAGE_EVENTS', 'VIEW_EVENT_REPORTS')")
    @GetMapping("/by-user/{userId}")
    public ResponseEntity<List<EventAttendance>> getByUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(attendanceService.getAttendanceByUser(userId));
    }

    @PreAuthorize("hasAnyRole('OWNER', 'PRIEST') " +
            "or @permissionEvaluator.hasAny(authentication, 'MANAGE_EVENTS', 'VIEW_EVENT_REPORTS')")
    @GetMapping("/by-user/{userId}/status/{status}")
    public ResponseEntity<List<EventAttendance>> getByUserAndStatus(@PathVariable UUID userId,
                                                                    @PathVariable AttendanceStatus status) {
        return ResponseEntity.ok(attendanceService.getAttendanceByUserAndStatus(userId, status));
    }

    private UUID resolveCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal userPrincipal) {
            return userPrincipal.getUserUuid();
        }
        throw new IllegalStateException("Unable to resolve the authenticated user");
    }
}
