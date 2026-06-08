package com.anastasia.Anastasia_BackEnd.modules.events.service;

import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.modules.events.model.EventEntity;
import com.anastasia.Anastasia_BackEnd.modules.events.model.attendance.AttendanceAttendeeType;
import com.anastasia.Anastasia_BackEnd.modules.events.model.attendance.AttendanceStatus;
import com.anastasia.Anastasia_BackEnd.modules.events.model.attendance.EventAttendance;
import com.anastasia.Anastasia_BackEnd.modules.events.model.report.EventReport;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserType;
import com.anastasia.Anastasia_BackEnd.modules.events.repository.EventAttendanceRepository;
import com.anastasia.Anastasia_BackEnd.modules.events.repository.EventRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class EventReportService {

    private final EventRepository eventRepository;
    private final EventAttendanceRepository attendanceRepository;
    private final UserRepository userRepository;

    public EventReportService(EventRepository eventRepository, EventAttendanceRepository attendanceRepository, UserRepository userRepository) {
        this.eventRepository = eventRepository;
        this.attendanceRepository = attendanceRepository;
        this.userRepository = userRepository;
    }

    public EventReport generateEventReport(Long eventId) {
        UUID tenantId = requireTenantId();
        EventEntity event = findEventForTenant(eventId, tenantId);

        // 1. Event Attendance Summary
        EventReport.EventSummary eventSummary = generateEventSummary(event, tenantId);

        // 2. Attendance Over Time for recurring events
        List<EventReport.AttendanceOverTime> attendanceOverTime = generateAttendanceOverTime(event, tenantId);

        // 3. Individual User Attendance Report (for all users)
        List<EventReport.UserAttendanceReport> userAttendanceReports = generateUserAttendanceReports(event, tenantId);

        // 4. CSV/Excel Data
        String csvData = generateCSVData(event);

        // 5. PDF Summary
        String pdfSummary = generatePDFSummary(event);

        return new EventReport(eventSummary, attendanceOverTime, userAttendanceReports, csvData, pdfSummary);
    }

    private EventReport.EventSummary generateEventSummary(EventEntity event, UUID tenantId) {
        List<EventAttendance> attendance = attendanceRepository.findByEventIdAndTenantId(event.getEventId(), tenantId);
        int invitedCount = attendance.size();
        int checkedInCount = (int) attendance.stream().filter(attendance1 -> attendance1.getStatus() == AttendanceStatus.CHECKED_IN).count();
        int absentCount = invitedCount - checkedInCount;

        double attendanceRate = invitedCount > 0 ? (double) checkedInCount / invitedCount : 0d;

        return new EventReport.EventSummary(
                event.getTitle(),
                toLocalDate(event.getStartAt(), event.getTimezone()),
                invitedCount,
                checkedInCount,
                absentCount,
                attendanceRate
        );
    }

    private List<EventReport.AttendanceOverTime> generateAttendanceOverTime(EventEntity event, UUID tenantId) {
        List<EventAttendance> attendanceList = attendanceRepository.findByEventIdAndTenantId(event.getEventId(), tenantId);

        // Group attendance by event date
        Map<LocalDate, List<EventAttendance>> attendanceByDate = attendanceList.stream()
                .collect(Collectors.groupingBy(attendance ->
                        toLocalDate(attendance.getEvent().getStartAt(), attendance.getEvent().getTimezone())));

        // Generate the attendance over time
        return attendanceByDate.entrySet().stream()
                .map(entry -> new EventReport.AttendanceOverTime(
                        entry.getKey(),
                        (int) entry.getValue().stream().filter(att -> att.getStatus() == AttendanceStatus.CHECKED_IN).count(),
                        (int) entry.getValue().stream().filter(att -> att.getStatus() == AttendanceStatus.ABSENT).count()
                ))
                .collect(Collectors.toList());
    }

    private List<EventReport.UserAttendanceReport> generateUserAttendanceReports(EventEntity event, UUID tenantId) {
        List<EventAttendance> attendanceList = attendanceRepository.findByEventIdAndTenantId(event.getEventId(), tenantId);

        Map<AttendeeGroupKey, List<EventAttendance>> attendanceByUser = attendanceList.stream()
                .collect(Collectors.groupingBy(this::buildAttendeeGroupKey));

        return attendanceByUser.entrySet().stream()
                .map(entry -> {
                    UUID userId = entry.getKey().userId();
                    List<EventAttendance> userAttendance = entry.getValue();
                    int totalInvited = userAttendance.size();
                    int totalAttended = (int) userAttendance.stream().filter(att -> att.getStatus() == AttendanceStatus.CHECKED_IN).count();
                    int totalMissed = totalInvited - totalAttended;

                    double attendanceRate = totalInvited > 0 ? (double) totalAttended / totalInvited : 0d;

                    List<EventReport.UserAttendanceReport.AttendanceStatusBreakdown> statusBreakdown = generateUserStatusBreakdown(userAttendance);

                    return new EventReport.UserAttendanceReport(
                            userId,
                            resolveAttendeeName(userId, userAttendance, tenantId),
                            resolveAttendeeType(userAttendance),
                            totalInvited,
                            totalAttended,
                            totalMissed,
                            attendanceRate,
                            statusBreakdown
                    );
                }).toList();
    }

    private AttendeeGroupKey buildAttendeeGroupKey(EventAttendance attendance) {
        if (attendance.getUser() != null) {
            return new AttendeeGroupKey(attendance.getUser().getUuid(), null, null, null);
        }
        return new AttendeeGroupKey(
                null,
                normalizeGuestValue(attendance.getGuestFullName()),
                normalizeGuestValue(attendance.getGuestEmail()),
                normalizeGuestValue(attendance.getGuestPhone())
        );
    }

    private EventEntity findEventForTenant(Long eventId, UUID tenantId) {
        return eventRepository.findByEventIdAndTenantId(eventId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found"));
    }

    private String resolveAttendeeName(UUID userId, List<EventAttendance> userAttendance, UUID tenantId) {
        if (userId != null) {
            return userRepository.findByUuidAndAffiliatedTenantId(userId, tenantId)
                    .map(UserEntity::getFullName)
                    .orElse("Unknown");
        }
        EventAttendance sample = userAttendance.isEmpty() ? null : userAttendance.get(0);
        if (sample == null) {
            return "Unknown";
        }
        if (sample.getGuestFullName() != null && !sample.getGuestFullName().isBlank()) {
            return sample.getGuestFullName();
        }
        if (sample.getGuestEmail() != null && !sample.getGuestEmail().isBlank()) {
            return sample.getGuestEmail();
        }
        return "Guest";
    }

    private AttendanceAttendeeType resolveAttendeeType(List<EventAttendance> userAttendance) {
        EventAttendance sample = userAttendance.isEmpty() ? null : userAttendance.get(0);
        if (sample == null || sample.getUser() == null) {
            return AttendanceAttendeeType.GUEST;
        }
        UserType userType = sample.getUser().getUserType();
        if (userType == null) {
            return AttendanceAttendeeType.USER;
        }
        return switch (userType) {
            case MEMBER -> AttendanceAttendeeType.MEMBER;
            case STAFF -> AttendanceAttendeeType.STAFF;
            case PRIEST -> AttendanceAttendeeType.PRIEST;
            case GUEST -> AttendanceAttendeeType.GUEST;
            default -> AttendanceAttendeeType.USER;
        };
    }

    private UUID requireTenantId() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant ID not found in context");
        }
        return tenantId;
    }

    private List<EventReport.UserAttendanceReport.AttendanceStatusBreakdown> generateUserStatusBreakdown(List<EventAttendance> userAttendance) {
        Map<AttendanceStatus, Long> statusCounts = userAttendance.stream()
                .collect(Collectors.groupingBy(EventAttendance::getStatus, Collectors.counting()));

        return statusCounts.entrySet().stream()
                .map(entry -> new EventReport.UserAttendanceReport.AttendanceStatusBreakdown(entry.getKey(), entry.getValue().intValue()))
                .collect(Collectors.toList());
    }

    private String generateCSVData(EventEntity event) {
        // Implementation for generating CSV/Excel data
        return "CSV data here...";
    }

    private String generatePDFSummary(EventEntity event) {
        // Implementation for generating PDF summary
        return "PDF summary here...";
    }

    private LocalDate toLocalDate(Instant instant, String timezone) {
        if (instant == null) {
            return null;
        }
        return instant.atZone(resolveZone(timezone)).toLocalDate();
    }

    private ZoneId resolveZone(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            return ZoneId.of("UTC");
        }
        return ZoneId.of(timezone);
    }

    private String normalizeGuestValue(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private record AttendeeGroupKey(UUID userId, String guestFullName, String guestEmail, String guestPhone) {
    }
}
