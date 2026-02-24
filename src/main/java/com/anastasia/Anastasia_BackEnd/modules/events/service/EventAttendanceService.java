package com.anastasia.Anastasia_BackEnd.modules.events.service;

import com.anastasia.Anastasia_BackEnd.modules.events.model.EventEntity;
import com.anastasia.Anastasia_BackEnd.modules.events.model.attendance.AttendanceStatus;
import com.anastasia.Anastasia_BackEnd.modules.events.model.attendance.CheckInRequestDTO;
import com.anastasia.Anastasia_BackEnd.modules.events.model.attendance.EventAttendance;
import com.anastasia.Anastasia_BackEnd.modules.events.model.attendance.MarkAbsentRequestDTO;
import com.anastasia.Anastasia_BackEnd.modules.events.model.attendance.UpdateAttendanceStatusRequestDTO;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.modules.events.repository.EventAttendanceRepository;
import com.anastasia.Anastasia_BackEnd.modules.events.repository.EventRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventAttendanceService {

    private final UserRepository userRepository;
    private final EventAttendanceRepository attendanceRepository;
    private final EventRepository eventRepository;

    public EventAttendance checkIn(CheckInRequestDTO requestDTO) {

        EventEntity event = eventRepository.findById(requestDTO.getEventId())
                .orElseThrow(() -> new EntityNotFoundException("Event not valid"));

        UserEntity user = userRepository.findById(requestDTO.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        boolean alreadyCheckIn = attendanceRepository
                .findByUserUuidAndEventId(user.getUuid(), event.getEventId())
                .isPresent();

        if(alreadyCheckIn){
            throw new IllegalStateException("User already checked in");
        }

        EventAttendance attendance = EventAttendance.builder()
                .event(event)
                .user(user)
                .checkInTime(LocalDateTime.now())
                .checkInMethod(requestDTO.getCheckInMethod())
                .status(AttendanceStatus.CHECKED_IN)
                .checkedInBy(requestDTO.getCheckedInBy())
                .build();

        return attendanceRepository.save(attendance);
    }

    public EventAttendance markAbsent(MarkAbsentRequestDTO request) {
        EventEntity event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new EntityNotFoundException("Event not found"));

        UserEntity user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        boolean alreadyMarked = attendanceRepository
                .findByUserUuidAndEventId(user.getUuid(), event.getEventId()).isPresent();

        if (alreadyMarked) {
            throw new IllegalStateException("Attendance already recorded");
        }

        EventAttendance attendance = EventAttendance.builder()
                .event(event)
                .user(user)
                .status(AttendanceStatus.ABSENT)
                .checkedInBy(request.getMarkedAbsentBy())
                .build();

        return attendanceRepository.save(attendance);
    }

    public List<EventAttendance> getAttendanceByEvent(Long eventId) {
        return attendanceRepository.findByEventId(eventId);
    }

    public List<EventAttendance> getAttendanceByUser(UUID userId) {
        return attendanceRepository.findByUserUuid(userId);
    }

    public List<EventAttendance> getAttendanceByEventAndStatus(Long eventId, AttendanceStatus status) {
        return attendanceRepository.findByEventIdAndStatus(eventId, status);
    }

    public List<EventAttendance> getAttendanceByUserAndStatus(UUID userId, AttendanceStatus status) {
        return attendanceRepository.findByUserUuidAndStatus(userId, status);
    }

    public EventAttendance updateAttendanceStatus(UpdateAttendanceStatusRequestDTO request) {
        EventEntity event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new EntityNotFoundException("Event not found"));

        UserEntity user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        EventAttendance attendance = attendanceRepository
                .findByUserUuidAndEventId(user.getUuid(), event.getEventId())
                .orElseGet(() -> EventAttendance.builder()
                        .event(event)
                        .user(user)
                        .build());

        attendance.setStatus(request.getStatus());
        attendance.setCheckInMethod(request.getCheckInMethod());
        attendance.setCheckedInBy(request.getUpdatedBy());
        if (request.getStatus() == AttendanceStatus.CHECKED_IN || request.getStatus() == AttendanceStatus.LATE) {
            attendance.setCheckInTime(LocalDateTime.now());
        }

        return attendanceRepository.save(attendance);
    }
}
