package com.anastasia.Anastasia_BackEnd.service.event;

import com.anastasia.Anastasia_BackEnd.model.event.EventEntity;
import com.anastasia.Anastasia_BackEnd.model.event.attendance.AttendanceStatus;
import com.anastasia.Anastasia_BackEnd.model.event.attendance.CheckInQRRequestDTO;
import com.anastasia.Anastasia_BackEnd.model.event.attendance.EventAttendance;
import com.anastasia.Anastasia_BackEnd.model.user.UserEntity;
import com.anastasia.Anastasia_BackEnd.repository.EventAttendanceRepository;
import com.anastasia.Anastasia_BackEnd.repository.EventRepository;
import com.anastasia.Anastasia_BackEnd.repository.auth.UserRepository;
import com.anastasia.Anastasia_BackEnd.util.AttendanceTimeValidator;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class QrCheckInService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final EventAttendanceRepository attendanceRepository;
    private final AttendanceTimeValidator attendanceTimeValidator;

    private static final double MAX_DISTANCE_METERS = 100; // 100 meters radius

    public EventAttendance checkInWithQR(CheckInQRRequestDTO request) {
        EventEntity event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new EntityNotFoundException("Event not found"));

        UserEntity user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        boolean alreadyCheckedIn = attendanceRepository
                .findByUserUuidAndEventId(user.getUuid(), event.getEventId())
                .isPresent();

        if (alreadyCheckedIn) {
            throw new IllegalStateException("User already checked in");
        }

        if (event.getLatitude() == null || event.getLongitude() == null) {
            throw new IllegalStateException("Event location not set");
        }

        double distance = calculateDistance(
                event.getLatitude(), event.getLongitude(),
                request.getLatitude(), request.getLongitude()
        );

        if (distance > MAX_DISTANCE_METERS) {
            throw new IllegalStateException("You are not within the check-in area");
        }

        if(!attendanceTimeValidator.isCheckInAllowed(event)){
            throw new IllegalStateException("Check-in not allowed at this time");
        }

        EventAttendance attendance = EventAttendance.builder()
                .event(event)
                .user(user)
                .checkInTime(LocalDateTime.now())
                .checkInMethod("QR")
                .status(AttendanceStatus.CHECKED_IN)
                .checkedInBy(request.getUserId())
                .build();

        return attendanceRepository.save(attendance);
    }

    // Check If the user is within 100 meters of the event location using haversine formula.
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000; // Earth radius in meters
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
