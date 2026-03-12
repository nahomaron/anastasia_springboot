package com.anastasia.Anastasia_BackEnd.UnitTests.service.event;

import com.anastasia.Anastasia_BackEnd.common.i18n.LocalizedMessageService;
import com.anastasia.Anastasia_BackEnd.modules.events.model.EventEntity;
import com.anastasia.Anastasia_BackEnd.modules.events.model.attendance.AttendanceStatus;
import com.anastasia.Anastasia_BackEnd.modules.events.model.attendance.CheckInQRRequestDTO;
import com.anastasia.Anastasia_BackEnd.modules.events.model.attendance.EventAttendance;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.modules.events.repository.EventAttendanceRepository;
import com.anastasia.Anastasia_BackEnd.modules.events.repository.EventRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import com.anastasia.Anastasia_BackEnd.modules.events.service.QrCheckInService;
import com.anastasia.Anastasia_BackEnd.common.utils.AttendanceTimeValidator;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class QrCheckInServiceUnitTest {

    @Mock
    private EventRepository eventRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EventAttendanceRepository attendanceRepository;
    @Mock
    private AttendanceTimeValidator attendanceTimeValidator;
    @Mock
    private LocalizedMessageService messageService;

    @InjectMocks
    private QrCheckInService qrCheckInService;

    private EventEntity event;
    private UserEntity user;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = UserEntity.builder()
                .uuid(userId)
                .fullName("Qr User")
                .build();

        lenient().doAnswer(invocation -> invocation.getArgument(1))
                .when(messageService)
                .get(anyString(), anyString(), any());

        event = EventEntity.builder()
                .eventId(15L)
                .title("Youth Fellowship")
                .startAt(LocalDate.now().atTime(9, 0).toInstant(ZoneOffset.UTC))
                .endAt(LocalDate.now().atTime(11, 0).toInstant(ZoneOffset.UTC))
                .allowGeoCheckIn(true)
                .latitude(8.9806)
                .longitude(38.7578)
                .build();
    }

    @Test
    void checkInWithQR_whenValid_shouldPersistAttendance() {
        CheckInQRRequestDTO request = CheckInQRRequestDTO.builder()
                .eventId(event.getEventId())
                .userId(userId)
                .latitude(8.9807)
                .longitude(38.7579)
                .build();

        when(eventRepository.findById(event.getEventId())).thenReturn(Optional.of(event));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(attendanceRepository.findByUserUuidAndEventId(userId, event.getEventId()))
                .thenReturn(Optional.empty());
        when(attendanceTimeValidator.isCheckInAllowed(event)).thenReturn(true);
        when(attendanceRepository.save(any(EventAttendance.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        EventAttendance attendance = qrCheckInService.checkInWithQR(request);

        assertThat(attendance.getStatus()).isEqualTo(AttendanceStatus.CHECKED_IN);
        assertThat(attendance.getCheckInMethod()).isEqualTo("QR");
        assertThat(attendance.getEvent()).isEqualTo(event);
        assertThat(attendance.getUser()).isEqualTo(user);
        assertThat(attendance.getCheckInTime()).isNotNull();
        assertThat(attendance.getCheckedInBy()).isEqualTo(userId);
    }

    @Test
    void checkInWithQR_whenEventMissing_shouldThrow() {
        when(eventRepository.findById(event.getEventId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> qrCheckInService.checkInWithQR(buildRequest()))
                .isInstanceOf(EntityNotFoundException.class);

        verify(attendanceRepository, never()).save(any());
    }

    @Test
    void checkInWithQR_whenUserMissing_shouldThrow() {
        when(eventRepository.findById(event.getEventId())).thenReturn(Optional.of(event));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> qrCheckInService.checkInWithQR(buildRequest()))
                .isInstanceOf(EntityNotFoundException.class);

        verify(attendanceRepository, never()).save(any());
    }

    @Test
    void checkInWithQR_whenAlreadyCheckedIn_shouldThrow() {
        when(eventRepository.findById(event.getEventId())).thenReturn(Optional.of(event));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(attendanceRepository.findByUserUuidAndEventId(userId, event.getEventId()))
                .thenReturn(Optional.of(EventAttendance.builder().build()));

        assertThatThrownBy(() -> qrCheckInService.checkInWithQR(buildRequest()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void checkInWithQR_whenEventLocationMissing_shouldThrow() {
        event.setLatitude(null);
        event.setLongitude(null);

        when(eventRepository.findById(event.getEventId())).thenReturn(Optional.of(event));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(attendanceRepository.findByUserUuidAndEventId(userId, event.getEventId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> qrCheckInService.checkInWithQR(buildRequest()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void checkInWithQR_whenOutsideAllowedRadius_shouldThrow() {
        CheckInQRRequestDTO request = CheckInQRRequestDTO.builder()
                .eventId(event.getEventId())
                .userId(userId)
                .latitude(9.5)
                .longitude(39.5)
                .build();

        when(eventRepository.findById(event.getEventId())).thenReturn(Optional.of(event));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(attendanceRepository.findByUserUuidAndEventId(userId, event.getEventId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> qrCheckInService.checkInWithQR(request))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void checkInWithQR_whenCheckInNotAllowed_shouldThrow() {
        when(eventRepository.findById(event.getEventId())).thenReturn(Optional.of(event));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(attendanceRepository.findByUserUuidAndEventId(userId, event.getEventId()))
                .thenReturn(Optional.empty());
        when(attendanceTimeValidator.isCheckInAllowed(event)).thenReturn(false);

        assertThatThrownBy(() -> qrCheckInService.checkInWithQR(buildRequest()))
                .isInstanceOf(IllegalStateException.class);
    }

    private CheckInQRRequestDTO buildRequest() {
        return CheckInQRRequestDTO.builder()
                .eventId(event.getEventId())
                .userId(userId)
                .latitude(8.9806)
                .longitude(38.7578)
                .build();
    }
}
