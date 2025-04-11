package com.anastasia.Anastasia_BackEnd.model.event.report;

import com.anastasia.Anastasia_BackEnd.model.event.attendance.AttendanceStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EventReport {

    private EventSummary eventSummary;
    private List<AttendanceOverTime> attendanceOverTime;
    private List<UserAttendanceReport> userAttendanceReport;

    // Additional data for exportable formats
    private String csvData; // For CSV/Excel export
    private String pdfSummary; // For PDF summary generation

    // Nested classes to represent the report details
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class EventSummary {
        private String eventName;
        private LocalDate eventDate;
        private int invitedCount;
        private int checkedInCount;
        private int absentCount;
        private double attendanceRate;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AttendanceOverTime {
        private LocalDate eventDate;
        private int checkedInCount;
        private int absentCount;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserAttendanceReport {
        private UUID userId;
        private String userName;
        private int totalInvited;
        private int totalAttended;
        private int totalMissed;
        private double attendanceRate;
        private List<AttendanceStatusBreakdown> statusBreakdown;

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        public static class AttendanceStatusBreakdown {
            private AttendanceStatus status;
            private int count;
        }
    }
}

