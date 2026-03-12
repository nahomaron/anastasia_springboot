package com.anastasia.Anastasia_BackEnd.modules.events.service;

import com.anastasia.Anastasia_BackEnd.modules.events.model.EventEntity;
import com.anastasia.Anastasia_BackEnd.modules.events.model.attendance.AttendanceStatus;
import com.anastasia.Anastasia_BackEnd.modules.events.model.attendance.EventAttendance;
import com.anastasia.Anastasia_BackEnd.modules.events.model.report.EventReport;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.modules.events.repository.EventAttendanceRepository;
import com.anastasia.Anastasia_BackEnd.modules.events.repository.EventRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
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
        EventEntity event = eventRepository.findById(eventId).orElseThrow(() -> new RuntimeException("Event not found"));

        // 1. Event Attendance Summary
        EventReport.EventSummary eventSummary = generateEventSummary(event);

        // 2. Attendance Over Time for recurring events
        List<EventReport.AttendanceOverTime> attendanceOverTime = generateAttendanceOverTime(event);

        // 3. Individual User Attendance Report (for all users)
        List<EventReport.UserAttendanceReport> userAttendanceReports = generateUserAttendanceReports(event);

        // 4. CSV/Excel Data
        String csvData = generateCSVData(event);

        // 5. PDF Summary
        String pdfSummary = generatePDFSummary(event);

        return new EventReport(eventSummary, attendanceOverTime, userAttendanceReports, csvData, pdfSummary);
    }

    private EventReport.EventSummary generateEventSummary(EventEntity event) {
        List<EventAttendance> attendance = attendanceRepository.findByEventId(event.getEventId());
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

    private List<EventReport.AttendanceOverTime> generateAttendanceOverTime(EventEntity event) {
        List<EventAttendance> attendanceList = attendanceRepository.findByEventId(event.getEventId());

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

    private List<EventReport.UserAttendanceReport> generateUserAttendanceReports(EventEntity event) {
        List<EventAttendance> attendanceList = attendanceRepository.findByEventId(event.getEventId());

        // Group attendance by user
        Map<UUID, List<EventAttendance>> attendanceByUser = attendanceList.stream()
                .collect(Collectors.groupingBy(attendance -> attendance.getUser().getUuid()));

        // Now returning a list of reports for all users
        return attendanceByUser.entrySet().stream()
                .map(entry -> {
                    UUID userId = entry.getKey();
                    List<EventAttendance> userAttendance = entry.getValue();
                    int totalInvited = userAttendance.size();
                    int totalAttended = (int) userAttendance.stream().filter(att -> att.getStatus() == AttendanceStatus.CHECKED_IN).count();
                    int totalMissed = totalInvited - totalAttended;

                    double attendanceRate = totalInvited > 0 ? (double) totalAttended / totalInvited : 0d;

                    List<EventReport.UserAttendanceReport.AttendanceStatusBreakdown> statusBreakdown = generateUserStatusBreakdown(userAttendance);

                    return new EventReport.UserAttendanceReport(
                            userId,
                            userRepository.findById(userId).map(UserEntity::getFullName).orElse("Unknown"),
                            totalInvited,
                            totalAttended,
                            totalMissed,
                            attendanceRate,
                            statusBreakdown
                    );
                }).toList();
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
}
