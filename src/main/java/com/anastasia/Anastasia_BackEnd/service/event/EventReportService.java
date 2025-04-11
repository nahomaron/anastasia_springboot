package com.anastasia.Anastasia_BackEnd.service.event;

import com.anastasia.Anastasia_BackEnd.model.event.EventEntity;
import com.anastasia.Anastasia_BackEnd.model.event.attendance.AttendanceStatus;
import com.anastasia.Anastasia_BackEnd.model.event.attendance.EventAttendance;
import com.anastasia.Anastasia_BackEnd.model.event.report.EventReport;
import com.anastasia.Anastasia_BackEnd.model.user.UserEntity;
import com.anastasia.Anastasia_BackEnd.repository.EventAttendanceRepository;
import com.anastasia.Anastasia_BackEnd.repository.EventRepository;
import com.anastasia.Anastasia_BackEnd.repository.auth.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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

        double attendanceRate = (double) checkedInCount / invitedCount * 100;

        return new EventReport.EventSummary(
                event.getTitle(),
                event.getDate(),
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
                .collect(Collectors.groupingBy(attendance -> attendance.getEvent().getDate()));

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

                    double attendanceRate = (double) totalAttended / totalInvited * 100;

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
}
