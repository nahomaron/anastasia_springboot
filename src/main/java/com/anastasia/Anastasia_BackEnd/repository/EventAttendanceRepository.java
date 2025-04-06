package com.anastasia.Anastasia_BackEnd.repository;

import com.anastasia.Anastasia_BackEnd.model.event.attendance.AttendanceStatus;
import com.anastasia.Anastasia_BackEnd.model.event.attendance.EventAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventAttendanceRepository extends JpaRepository<EventAttendance, Long> {
    Optional<EventAttendance> findByUserUuidAndEventId(UUID userId, Long eventId);

    List<EventAttendance> findByEventId(Long eventId);

    List<EventAttendance> findByUserUuid(UUID userId);

    List<EventAttendance> findByEventIdAndStatus(Long eventId, AttendanceStatus status);

    List<EventAttendance> findByUserUuidAndStatus(UUID userId, AttendanceStatus status);
}
