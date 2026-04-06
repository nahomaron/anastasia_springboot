package com.anastasia.Anastasia_BackEnd.UnitTests.service;

import com.anastasia.Anastasia_BackEnd.common.i18n.LocalizedMessageService;
import com.anastasia.Anastasia_BackEnd.modules.events.model.EventEntity;
import com.anastasia.Anastasia_BackEnd.modules.events.model.attendance.AttendanceStatus;
import com.anastasia.Anastasia_BackEnd.modules.events.model.attendance.CheckInRequestDTO;
import com.anastasia.Anastasia_BackEnd.modules.events.model.attendance.EventAttendance;
import com.anastasia.Anastasia_BackEnd.modules.events.model.attendance.MarkAbsentRequestDTO;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.modules.events.repository.EventAttendanceRepository;
import com.anastasia.Anastasia_BackEnd.modules.events.repository.EventRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import com.anastasia.Anastasia_BackEnd.modules.events.service.EventAttendanceService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import com.anastasia.Anastasia_BackEnd.UnitTests.support.LenientMockitoTest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@LenientMockitoTest
class EventAttendanceServiceUnitTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private EventAttendanceRepository attendanceRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private LocalizedMessageService messageService;

    @InjectMocks
    private EventAttendanceService eventAttendanceService;

    private EventEntity event;
    private UserEntity user;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        event = EventEntity.builder().eventId(42L).build();
        user = UserEntity.builder().uuid(userId).build();
        lenient().when(messageService.get(anyString(), anyString()))
                .thenAnswer(invocation -> invocation.getArgument(1));
        lenient().when(messageService.get(anyString(), anyString(), any(Object[].class)))
                .thenAnswer(invocation -> invocation.getArgument(1));
    }

    @Test
    void checkIn_whenValidRequest_savesAttendance() {
        CheckInRequestDTO request = CheckInRequestDTO.builder()
                .eventId(event.getEventId())
                .userId(userId)
                .checkInMethod("QR")
                .checkedInBy(UUID.randomUUID())
                .build();

        when(eventRepository.findById(event.getEventId())).thenReturn(Optional.of(event));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(attendanceRepository.findByUserUuidAndEventId(userId, event.getEventId()))
                .thenReturn(Optional.empty());
        when(attendanceRepository.save(any(EventAttendance.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        EventAttendance result = eventAttendanceService.checkIn(request);

        assertThat(result.getEvent()).isEqualTo(event);
        assertThat(result.getUser()).isEqualTo(user);
        assertThat(result.getStatus()).isEqualTo(AttendanceStatus.CHECKED_IN);
        assertThat(result.getCheckInMethod()).isEqualTo("QR");
        verify(attendanceRepository).save(any(EventAttendance.class));
    }

    @Test
    void checkIn_whenEventMissing_throwsEntityNotFound() {
        CheckInRequestDTO request = CheckInRequestDTO.builder()
                .eventId(event.getEventId())
                .userId(userId)
                .build();

        when(eventRepository.findById(event.getEventId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventAttendanceService.checkIn(request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Event not valid");

        verify(attendanceRepository, never()).save(any());
    }

    @Test
    void checkIn_whenUserMissing_throwsEntityNotFound() {
        CheckInRequestDTO request = CheckInRequestDTO.builder()
                .eventId(event.getEventId())
                .userId(userId)
                .build();

        when(eventRepository.findById(event.getEventId())).thenReturn(Optional.of(event));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventAttendanceService.checkIn(request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("User not found");

        verify(attendanceRepository, never()).save(any());
    }

    @Test
    void checkIn_whenAlreadyCheckedIn_throwsIllegalState() {
        CheckInRequestDTO request = CheckInRequestDTO.builder()
                .eventId(event.getEventId())
                .userId(userId)
                .build();

        when(eventRepository.findById(event.getEventId())).thenReturn(Optional.of(event));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(attendanceRepository.findByUserUuidAndEventId(userId, event.getEventId()))
                .thenReturn(Optional.of(EventAttendance.builder().build()));

        assertThatThrownBy(() -> eventAttendanceService.checkIn(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("User already checked in");

        verify(attendanceRepository, never()).save(any());
    }

    @Test
    void markAbsent_whenValidRequest_savesAttendance() {
        MarkAbsentRequestDTO request = MarkAbsentRequestDTO.builder()
                .eventId(event.getEventId())
                .userId(userId)
                .markedAbsentBy(UUID.randomUUID())
                .build();

        when(eventRepository.findById(event.getEventId())).thenReturn(Optional.of(event));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(attendanceRepository.findByUserUuidAndEventId(userId, event.getEventId()))
                .thenReturn(Optional.empty());
        when(attendanceRepository.save(any(EventAttendance.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        EventAttendance result = eventAttendanceService.markAbsent(request);

        assertThat(result.getStatus()).isEqualTo(AttendanceStatus.ABSENT);
        assertThat(result.getCheckedInBy()).isEqualTo(request.getMarkedAbsentBy());
        verify(attendanceRepository).save(any(EventAttendance.class));
    }

    @Test
    void markAbsent_whenAttendanceExists_throwsIllegalState() {
        MarkAbsentRequestDTO request = MarkAbsentRequestDTO.builder()
                .eventId(event.getEventId())
                .userId(userId)
                .build();

        when(eventRepository.findById(event.getEventId())).thenReturn(Optional.of(event));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(attendanceRepository.findByUserUuidAndEventId(userId, event.getEventId()))
                .thenReturn(Optional.of(EventAttendance.builder().build()));

        assertThatThrownBy(() -> eventAttendanceService.markAbsent(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Attendance already recorded");

        verify(attendanceRepository, never()).save(any());
    }

    @Test
    void markAbsent_whenEventMissing_throwsEntityNotFound() {
        MarkAbsentRequestDTO request = MarkAbsentRequestDTO.builder()
                .eventId(event.getEventId())
                .userId(userId)
                .build();

        when(eventRepository.findById(event.getEventId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventAttendanceService.markAbsent(request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Event not found");
    }

    @Test
    void getAttendanceDelegatesToRepository() {
        when(attendanceRepository.findByEventId(event.getEventId()))
                .thenReturn(List.of(EventAttendance.builder().build()));
        when(attendanceRepository.findByUserUuid(userId))
                .thenReturn(List.of(EventAttendance.builder().build()));
        when(attendanceRepository.findByEventIdAndStatus(event.getEventId(), AttendanceStatus.CHECKED_IN))
                .thenReturn(List.of(EventAttendance.builder().status(AttendanceStatus.CHECKED_IN).build()));
        when(attendanceRepository.findByUserUuidAndStatus(userId, AttendanceStatus.ABSENT))
                .thenReturn(List.of(EventAttendance.builder().status(AttendanceStatus.ABSENT).build()));

        assertThat(eventAttendanceService.getAttendanceByEvent(event.getEventId())).hasSize(1);
        assertThat(eventAttendanceService.getAttendanceByUser(userId)).hasSize(1);
        assertThat(eventAttendanceService.getAttendanceByEventAndStatus(event.getEventId(), AttendanceStatus.CHECKED_IN))
                .hasSize(1);
        assertThat(eventAttendanceService.getAttendanceByUserAndStatus(userId, AttendanceStatus.ABSENT))
                .hasSize(1);

        verify(attendanceRepository).findByEventId(event.getEventId());
        verify(attendanceRepository).findByUserUuid(userId);
        verify(attendanceRepository).findByEventIdAndStatus(event.getEventId(), AttendanceStatus.CHECKED_IN);
        verify(attendanceRepository).findByUserUuidAndStatus(userId, AttendanceStatus.ABSENT);
    }
}
