package com.anastasia.Anastasia_BackEnd.modules.events.repository;

import com.anastasia.Anastasia_BackEnd.modules.events.model.attendance.AttendanceStatus;
import com.anastasia.Anastasia_BackEnd.modules.events.model.attendance.EventAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventAttendanceRepository extends JpaRepository<EventAttendance, Long> {
    Optional<EventAttendance> findByUserUuidAndEventId(UUID userId, Long eventId);

    @Query("""
            select attendance
            from EventAttendance attendance
            where attendance.id = :attendanceId
              and attendance.event.tenantId = :tenantId
            """)
    Optional<EventAttendance> findByIdAndEventTenantId(@Param("attendanceId") Long attendanceId, @Param("tenantId") UUID tenantId);

    @Query("""
            select attendance
            from EventAttendance attendance
            where attendance.event.eventId = :eventId
              and attendance.event.tenantId = :tenantId
            """)
    List<EventAttendance> findByEventIdAndTenantId(@Param("eventId") Long eventId, @Param("tenantId") UUID tenantId);

    @Query("""
            select attendance
            from EventAttendance attendance
            where attendance.user.uuid = :userId
              and attendance.event.tenantId = :tenantId
            """)
    List<EventAttendance> findByUserUuidAndTenantId(@Param("userId") UUID userId, @Param("tenantId") UUID tenantId);

    @Query("""
            select attendance
            from EventAttendance attendance
            join fetch attendance.event event
            where attendance.user.uuid = :userId
              and event.tenantId = :tenantId
            order by event.startAt desc, attendance.id desc
            """)
    List<EventAttendance> findDetailedByUserUuidAndTenantId(@Param("userId") UUID userId, @Param("tenantId") UUID tenantId);

    @Query("""
            select attendance
            from EventAttendance attendance
            where attendance.event.eventId = :eventId
              and attendance.status = :status
              and attendance.event.tenantId = :tenantId
            """)
    List<EventAttendance> findByEventIdAndStatusAndTenantId(@Param("eventId") Long eventId,
                                                            @Param("status") AttendanceStatus status,
                                                            @Param("tenantId") UUID tenantId);

    @Query("""
            select attendance
            from EventAttendance attendance
            where attendance.user.uuid = :userId
              and attendance.status = :status
              and attendance.event.tenantId = :tenantId
            """)
    List<EventAttendance> findByUserUuidAndStatusAndTenantId(@Param("userId") UUID userId,
                                                             @Param("status") AttendanceStatus status,
                                                             @Param("tenantId") UUID tenantId);
}
