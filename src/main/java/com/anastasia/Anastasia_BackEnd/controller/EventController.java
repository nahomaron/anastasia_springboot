package com.anastasia.Anastasia_BackEnd.controller;

import com.anastasia.Anastasia_BackEnd.model.event.*;
import com.anastasia.Anastasia_BackEnd.model.event.attendance.*;
import com.anastasia.Anastasia_BackEnd.model.event.requests.AssignEventManagerRequest;
import com.anastasia.Anastasia_BackEnd.model.event.requests.EventManagerDTO;
import com.anastasia.Anastasia_BackEnd.service.event.EventAttendanceService;
import com.anastasia.Anastasia_BackEnd.service.event.EventService;
import com.anastasia.Anastasia_BackEnd.service.event.QrCheckInService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @PostMapping
    public ResponseEntity<?> createEvent(@RequestBody EventDTO eventDTO){
        EventEntity event = eventService.convertToEntity(eventDTO);
        EventEntity savedEvent = eventService.createEvent(event);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PutMapping("/{eventId}/update")
    public ResponseEntity<?> updateEvent(@PathVariable Long eventId, @RequestBody EventDTO eventDTO){
        EventEntity event = eventService.convertToEntity(eventDTO);
        EventEntity updatedEvent = eventService.updateEvent(eventId, event);
        return new ResponseEntity<>(eventService.convertToDTO(updatedEvent), HttpStatus.ACCEPTED);
    }

    @PostMapping("/{eventId}/managers")
    public ResponseEntity<?> assignManager(@PathVariable Long eventId,
                                                             @RequestBody AssignEventManagerRequest request) {
       eventService.assignManagerToEvent(eventId, request.getUserId(), request.getRole());
        return new ResponseEntity<>(HttpStatus.OK);

    }

    @DeleteMapping("/{managerId}/remove")
    public ResponseEntity<?> removeManager(@PathVariable Long eventId, @PathVariable UUID managerId) {
        eventService.removeManager(eventId, managerId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<EventManagerDTO>> listManagers(@PathVariable Long eventId) {
        List<EventManagerEntity> managers = eventService.getManagers(eventId);

        return new ResponseEntity<>(managers.stream()
                .map(eventService::convertToDTO)
                .toList(), HttpStatus.OK);
    }

    @DeleteMapping("/{eventId}")
    public ResponseEntity<?> deleteEvent(@PathVariable Long eventId){
        eventService.deleteEvent(eventId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/event/check-in")
    public ResponseEntity<EventAttendance> checkIn(@RequestBody CheckInRequestDTO requestDTO){
        EventAttendance attendance = attendanceService.checkIn(requestDTO);
        return new ResponseEntity<>(attendance, HttpStatus.OK);
    }

    @PostMapping("/event/check-in/qr-code")
    public ResponseEntity<EventAttendance> checkInWithQR(@RequestBody CheckInQRRequestDTO requestDTO){
        EventAttendance attendance = qrCheckInService.checkInWithQR(requestDTO);
        return new ResponseEntity<>(attendance, HttpStatus.OK);
    }


    @PostMapping("/mark-absent")
    public ResponseEntity<EventAttendance> markAbsent(@RequestBody MarkAbsentRequestDTO request) {
        EventAttendance attendance = attendanceService.markAbsent(request);
        return new ResponseEntity<>(attendance, HttpStatus.OK);
    }

    @GetMapping("/by-event/{eventId}")
    public ResponseEntity<List<EventAttendance>> getByEvent(@PathVariable Long eventId) {
        return ResponseEntity.ok(attendanceService.getAttendanceByEvent(eventId));
    }

    @GetMapping("/by-event/{eventId}/status/{status}")
    public ResponseEntity<List<EventAttendance>> getByEventAndStatus(@PathVariable Long eventId,
                                                                     @PathVariable AttendanceStatus status) {
        return ResponseEntity.ok(attendanceService.getAttendanceByEventAndStatus(eventId, status));
    }

    @GetMapping("/by-user/{userId}")
    public ResponseEntity<List<EventAttendance>> getByUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(attendanceService.getAttendanceByUser(userId));
    }

    @GetMapping("/by-user/{userId}/status/{status}")
    public ResponseEntity<List<EventAttendance>> getByUserAndStatus(@PathVariable UUID userId,
                                                                    @PathVariable AttendanceStatus status) {
        return ResponseEntity.ok(attendanceService.getAttendanceByUserAndStatus(userId, status));
    }



}
