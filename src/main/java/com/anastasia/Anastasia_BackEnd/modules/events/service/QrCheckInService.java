package com.anastasia.Anastasia_BackEnd.modules.events.service;

import com.anastasia.Anastasia_BackEnd.common.i18n.LocalizedMessageService;
import com.anastasia.Anastasia_BackEnd.modules.events.model.EventEntity;
import com.anastasia.Anastasia_BackEnd.modules.events.model.attendance.AttendanceStatus;
import com.anastasia.Anastasia_BackEnd.modules.events.model.attendance.CheckInQRRequestDTO;
import com.anastasia.Anastasia_BackEnd.modules.events.model.attendance.EventAttendance;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.modules.events.repository.EventAttendanceRepository;
import com.anastasia.Anastasia_BackEnd.modules.events.repository.EventRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import com.anastasia.Anastasia_BackEnd.common.utils.AttendanceTimeValidator;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class QrCheckInService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final EventAttendanceRepository attendanceRepository;
    private final AttendanceTimeValidator attendanceTimeValidator;
    private final LocalizedMessageService messageService;

    private static final double DEFAULT_MAX_DISTANCE_METERS = 100; // fallback radius

    public EventAttendance checkInWithQR(CheckInQRRequestDTO request) {
        EventEntity event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new EntityNotFoundException(messageService.get(
                        "events.notFound",
                        "Event not found"
                )));

        UserEntity user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new EntityNotFoundException(messageService.get(
                        "user.notFound",
                        "User not found"
                )));

        boolean alreadyCheckedIn = attendanceRepository
                .findByUserUuidAndEventId(user.getUuid(), event.getEventId())
                .isPresent();

        if (alreadyCheckedIn) {
            throw new IllegalStateException(messageService.get(
                    "events.attendance.alreadyCheckedIn",
                    "User already checked in"
            ));
        }

        boolean geoEnabled = Boolean.TRUE.equals(event.getAllowGeoCheckIn());
        if (geoEnabled) {
            if (event.getLatitude() == null || event.getLongitude() == null) {
                throw new IllegalStateException(messageService.get(
                        "events.location.notSet",
                        "Event location not set"
                ));
            }

            double distance = calculateDistance(
                    event.getLatitude(), event.getLongitude(),
                    request.getLatitude(), request.getLongitude()
            );

            double radius = event.getGeofenceRadiusMeters() != null
                    ? event.getGeofenceRadiusMeters()
                    : DEFAULT_MAX_DISTANCE_METERS;

            if (distance > radius) {
                throw new IllegalStateException(messageService.get(
                        "events.checkin.outsideArea",
                        "You are not within the check-in area"
                ));
            }
        }

        if(!attendanceTimeValidator.isCheckInAllowed(event)){
            throw new IllegalStateException(messageService.get(
                    "events.checkin.time.invalid",
                    "Check-in not allowed at this time"
            ));
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
