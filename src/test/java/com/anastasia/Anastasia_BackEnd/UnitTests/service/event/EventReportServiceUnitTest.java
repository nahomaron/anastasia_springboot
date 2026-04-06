package com.anastasia.Anastasia_BackEnd.UnitTests.service.event;

import com.anastasia.Anastasia_BackEnd.modules.events.model.EventEntity;
import com.anastasia.Anastasia_BackEnd.modules.events.model.attendance.AttendanceStatus;
import com.anastasia.Anastasia_BackEnd.modules.events.model.attendance.EventAttendance;
import com.anastasia.Anastasia_BackEnd.modules.events.model.report.EventReport;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.modules.events.repository.EventAttendanceRepository;
import com.anastasia.Anastasia_BackEnd.modules.events.repository.EventRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import com.anastasia.Anastasia_BackEnd.modules.events.service.EventReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import com.anastasia.Anastasia_BackEnd.UnitTests.support.LenientMockitoTest;

import java.time.LocalDate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@LenientMockitoTest
class EventReportServiceUnitTest {

    @Mock
    private EventRepository eventRepository;
    @Mock
    private EventAttendanceRepository attendanceRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private EventReportService eventReportService;

    private EventEntity event;
    private UserEntity attendeeOne;
    private UserEntity attendeeTwo;

    @BeforeEach
    void setUp() {
        event = EventEntity.builder()
                .eventId(1L)
                .title("Sunday Service")
                .startAt(LocalDate.now().atTime(9, 0).toInstant(ZoneOffset.UTC))
                .endAt(LocalDate.now().atTime(11, 0).toInstant(ZoneOffset.UTC))
                .build();

        attendeeOne = UserEntity.builder()
                .uuid(UUID.randomUUID())
                .fullName("Alice Attendee")
                .email("alice@example.com")
                .build();

        attendeeTwo = UserEntity.builder()
                .uuid(UUID.randomUUID())
                .fullName("Bob Believer")
                .email("bob@example.com")
                .build();
    }

    @Test
    void generateEventReport_shouldAggregateAttendanceData() {
        EventAttendance attendance1 = EventAttendance.builder()
                .event(event)
                .user(attendeeOne)
                .status(AttendanceStatus.CHECKED_IN)
                .checkInTime(LocalDateTime.now().minusMinutes(30))
                .build();

        EventAttendance attendance2 = EventAttendance.builder()
                .event(event)
                .user(attendeeTwo)
                .status(AttendanceStatus.ABSENT)
                .build();

        when(eventRepository.findById(event.getEventId())).thenReturn(Optional.of(event));
        when(attendanceRepository.findByEventId(event.getEventId()))
                .thenReturn(List.of(attendance1, attendance2));
        when(userRepository.findById(attendeeOne.getUuid())).thenReturn(Optional.of(attendeeOne));
        when(userRepository.findById(attendeeTwo.getUuid())).thenReturn(Optional.of(attendeeTwo));

        EventReport report = eventReportService.generateEventReport(event.getEventId());

        EventReport.EventSummary summary = report.getEventSummary();
        assertThat(summary.getEventName()).isEqualTo("Sunday Service");
        assertThat(summary.getInvitedCount()).isEqualTo(2);
        assertThat(summary.getCheckedInCount()).isEqualTo(1);
        assertThat(summary.getAbsentCount()).isEqualTo(1);
        assertThat(summary.getAttendanceRate()).isEqualTo(0.5);

        assertThat(report.getAttendanceOverTime()).hasSize(1);
        EventReport.AttendanceOverTime overTime = report.getAttendanceOverTime().get(0);
        assertThat(overTime.getCheckedInCount()).isEqualTo(1);
        assertThat(overTime.getAbsentCount()).isEqualTo(1);

        assertThat(report.getUserAttendanceReport()).hasSize(2);
        assertThat(report.getUserAttendanceReport())
                .anyMatch(u -> u.getUserName().equals("Alice Attendee") && u.getTotalAttended() == 1);
        assertThat(report.getCsvData()).isEqualTo("CSV data here...");
        assertThat(report.getPdfSummary()).isEqualTo("PDF summary here...");
    }

    @Test
    void generateEventReport_whenEventMissing_shouldThrow() {
        when(eventRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventReportService.generateEventReport(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Event not found");
    }

    @Test
    void generateEventReport_whenUserLookupFails_shouldFallbackToUnknownName() {
        EventAttendance attendance = EventAttendance.builder()
                .event(event)
                .user(UserEntity.builder().uuid(attendeeOne.getUuid()).build())
                .status(AttendanceStatus.CHECKED_IN)
                .build();

        when(eventRepository.findById(event.getEventId())).thenReturn(Optional.of(event));
        when(attendanceRepository.findByEventId(event.getEventId()))
                .thenReturn(List.of(attendance));
        when(userRepository.findById(attendeeOne.getUuid())).thenReturn(Optional.empty());

        EventReport report = eventReportService.generateEventReport(event.getEventId());

        assertThat(report.getUserAttendanceReport()).hasSize(1);
        assertThat(report.getUserAttendanceReport().get(0).getUserName()).isEqualTo("Unknown");
    }
}
