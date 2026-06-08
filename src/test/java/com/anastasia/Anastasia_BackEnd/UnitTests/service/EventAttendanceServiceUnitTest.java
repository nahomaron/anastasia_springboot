package com.anastasia.Anastasia_BackEnd.UnitTests.service;

import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.common.i18n.LocalizedMessageService;
import com.anastasia.Anastasia_BackEnd.modules.events.model.EventEntity;
import com.anastasia.Anastasia_BackEnd.modules.events.model.attendance.AttendanceAttendeeType;
import com.anastasia.Anastasia_BackEnd.modules.events.model.attendance.AttendanceStatus;
import com.anastasia.Anastasia_BackEnd.modules.events.model.attendance.CheckInRequestDTO;
import com.anastasia.Anastasia_BackEnd.modules.events.model.attendance.EventAttendance;
import com.anastasia.Anastasia_BackEnd.modules.events.model.attendance.EventAttendanceResponse;
import com.anastasia.Anastasia_BackEnd.modules.events.model.attendance.MarkAbsentRequestDTO;
import com.anastasia.Anastasia_BackEnd.modules.events.model.attendance.MemberAttendanceReportResponse;
import com.anastasia.Anastasia_BackEnd.modules.events.model.attendance.UpdateAttendanceStatusRequestDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.Adult_MemberEntity;
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
    private UUID actorUserId;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);
        userId = UUID.randomUUID();
        actorUserId = UUID.randomUUID();
        ChurchEntity church = ChurchEntity.builder().churchId(7L).build();
        event = EventEntity.builder().eventId(42L).tenantId(tenantId).church(church).build();
        user = UserEntity.builder()
                .uuid(userId)
                .affiliatedTenantId(tenantId)
                .membership(Adult_MemberEntity.builder().id(101L).churchId(church.getChurchId()).build())
                .build();
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
        when(userRepository.findByUuidAndAffiliatedTenantId(userId, tenantId)).thenReturn(Optional.of(user));
        when(attendanceRepository.findByUserUuidAndEventId(userId, event.getEventId()))
                .thenReturn(Optional.empty());
        when(attendanceRepository.save(any(EventAttendance.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        EventAttendance result = eventAttendanceService.checkIn(request, actorUserId);

        assertThat(result.getEvent()).isEqualTo(event);
        assertThat(result.getUser()).isEqualTo(user);
        assertThat(result.getStatus()).isEqualTo(AttendanceStatus.CHECKED_IN);
        assertThat(result.getCheckInMethod()).isEqualTo("QR");
        assertThat(result.getCheckedInBy()).isEqualTo(actorUserId);
        verify(attendanceRepository).save(any(EventAttendance.class));
    }

    @Test
    void checkIn_whenEventMissing_throwsEntityNotFound() {
        CheckInRequestDTO request = CheckInRequestDTO.builder()
                .eventId(event.getEventId())
                .userId(userId)
                .build();

        when(eventRepository.findById(event.getEventId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventAttendanceService.checkIn(request, actorUserId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Event not found");

        verify(attendanceRepository, never()).save(any());
    }

    @Test
    void checkIn_whenUserMissing_throwsEntityNotFound() {
        CheckInRequestDTO request = CheckInRequestDTO.builder()
                .eventId(event.getEventId())
                .userId(userId)
                .build();

        when(eventRepository.findById(event.getEventId())).thenReturn(Optional.of(event));
        when(userRepository.findByUuidAndAffiliatedTenantId(userId, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventAttendanceService.checkIn(request, actorUserId))
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
        when(userRepository.findByUuidAndAffiliatedTenantId(userId, tenantId)).thenReturn(Optional.of(user));
        when(attendanceRepository.findByUserUuidAndEventId(userId, event.getEventId()))
                .thenReturn(Optional.of(EventAttendance.builder().build()));

        assertThatThrownBy(() -> eventAttendanceService.checkIn(request, actorUserId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Attendance already recorded");

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
        when(userRepository.findByUuidAndAffiliatedTenantId(userId, tenantId)).thenReturn(Optional.of(user));
        when(attendanceRepository.findByUserUuidAndEventId(userId, event.getEventId()))
                .thenReturn(Optional.empty());
        when(attendanceRepository.save(any(EventAttendance.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        EventAttendance result = eventAttendanceService.markAbsent(request, actorUserId);

        assertThat(result.getStatus()).isEqualTo(AttendanceStatus.ABSENT);
        assertThat(result.getCheckedInBy()).isEqualTo(actorUserId);
        verify(attendanceRepository).save(any(EventAttendance.class));
    }

    @Test
    void markAbsent_whenAttendanceExists_throwsIllegalState() {
        MarkAbsentRequestDTO request = MarkAbsentRequestDTO.builder()
                .eventId(event.getEventId())
                .userId(userId)
                .build();

        when(eventRepository.findById(event.getEventId())).thenReturn(Optional.of(event));
        when(userRepository.findByUuidAndAffiliatedTenantId(userId, tenantId)).thenReturn(Optional.of(user));
        when(attendanceRepository.findByUserUuidAndEventId(userId, event.getEventId()))
                .thenReturn(Optional.of(EventAttendance.builder().build()));

        assertThatThrownBy(() -> eventAttendanceService.markAbsent(request, actorUserId))
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

        assertThatThrownBy(() -> eventAttendanceService.markAbsent(request, actorUserId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Event not found");
    }

    @Test
    void updateAttendanceStatus_usesAuthenticatedActorInsteadOfRequestField() {
        EventAttendance existingAttendance = EventAttendance.builder()
                .event(event)
                .user(user)
                .status(AttendanceStatus.ABSENT)
                .build();
        UpdateAttendanceStatusRequestDTO request = UpdateAttendanceStatusRequestDTO.builder()
                .eventId(event.getEventId())
                .userId(userId)
                .status(AttendanceStatus.CHECKED_IN)
                .checkInMethod("MANUAL")
                .updatedBy(UUID.randomUUID())
                .build();

        when(eventRepository.findById(event.getEventId())).thenReturn(Optional.of(event));
        when(userRepository.findByUuidAndAffiliatedTenantId(userId, tenantId)).thenReturn(Optional.of(user));
        when(attendanceRepository.findByUserUuidAndEventId(userId, event.getEventId()))
                .thenReturn(Optional.of(existingAttendance));
        when(attendanceRepository.save(any(EventAttendance.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        EventAttendance result = eventAttendanceService.updateAttendanceStatus(request, actorUserId);

        assertThat(result.getStatus()).isEqualTo(AttendanceStatus.CHECKED_IN);
        assertThat(result.getCheckedInBy()).isEqualTo(actorUserId);
        assertThat(result.getCheckInTime()).isNotNull();
        verify(attendanceRepository).save(existingAttendance);
    }

    @Test
    void getAttendanceDelegatesToRepository() {
        when(attendanceRepository.findByEventIdAndTenantId(event.getEventId(), tenantId))
                .thenReturn(List.of(EventAttendance.builder().build()));
        when(userRepository.findByUuidAndAffiliatedTenantId(userId, tenantId)).thenReturn(Optional.of(user));
        when(eventRepository.findById(event.getEventId())).thenReturn(Optional.of(event));
        when(attendanceRepository.findByUserUuidAndTenantId(userId, tenantId))
                .thenReturn(List.of(EventAttendance.builder().build()));
        when(attendanceRepository.findByEventIdAndStatusAndTenantId(event.getEventId(), AttendanceStatus.CHECKED_IN, tenantId))
                .thenReturn(List.of(EventAttendance.builder().status(AttendanceStatus.CHECKED_IN).build()));
        when(attendanceRepository.findByUserUuidAndStatusAndTenantId(userId, AttendanceStatus.ABSENT, tenantId))
                .thenReturn(List.of(EventAttendance.builder().status(AttendanceStatus.ABSENT).build()));

        assertThat(eventAttendanceService.getAttendanceByEvent(event.getEventId())).hasSize(1);
        assertThat(eventAttendanceService.getAttendanceByUser(userId)).hasSize(1);
        assertThat(eventAttendanceService.getAttendanceByEventAndStatus(event.getEventId(), AttendanceStatus.CHECKED_IN))
                .hasSize(1);
        assertThat(eventAttendanceService.getAttendanceByUserAndStatus(userId, AttendanceStatus.ABSENT))
                .hasSize(1);

        verify(attendanceRepository).findByEventIdAndTenantId(event.getEventId(), tenantId);
        verify(attendanceRepository).findByUserUuidAndTenantId(userId, tenantId);
        verify(attendanceRepository).findByEventIdAndStatusAndTenantId(event.getEventId(), AttendanceStatus.CHECKED_IN, tenantId);
        verify(attendanceRepository).findByUserUuidAndStatusAndTenantId(userId, AttendanceStatus.ABSENT, tenantId);
    }

    @Test
    void checkIn_whenEventOutsideTenant_throwsEntityNotFound() {
        event.setTenantId(UUID.randomUUID());
        CheckInRequestDTO request = CheckInRequestDTO.builder()
                .eventId(event.getEventId())
                .userId(userId)
                .build();

        when(eventRepository.findById(event.getEventId())).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> eventAttendanceService.checkIn(request, actorUserId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Event not found");

        verify(userRepository, never()).findByUuidAndAffiliatedTenantId(any(), any());
        verify(attendanceRepository, never()).save(any());
    }

    @Test
    void checkIn_whenUserOutsideEventChurchScope_throwsIllegalArgument() {
        CheckInRequestDTO request = CheckInRequestDTO.builder()
                .eventId(event.getEventId())
                .userId(userId)
                .build();
        user.setMembership(Adult_MemberEntity.builder().churchId(99L).build());

        when(eventRepository.findById(event.getEventId())).thenReturn(Optional.of(event));
        when(userRepository.findByUuidAndAffiliatedTenantId(userId, tenantId)).thenReturn(Optional.of(user));

        when(attendanceRepository.findByUserUuidAndEventId(userId, event.getEventId()))
                .thenReturn(Optional.empty());
        when(attendanceRepository.save(any(EventAttendance.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        EventAttendance result = eventAttendanceService.checkIn(request, actorUserId);

        assertThat(result.getUser()).isEqualTo(user);
        assertThat(result.getStatus()).isEqualTo(AttendanceStatus.CHECKED_IN);
        verify(attendanceRepository).save(any(EventAttendance.class));
    }

    @Test
    void checkIn_whenGuestRequest_savesGuestAttendance() {
        CheckInRequestDTO request = CheckInRequestDTO.builder()
                .eventId(event.getEventId())
                .guestFullName("Guest Visitor")
                .guestEmail("guest@example.com")
                .guestPhone("555-0100")
                .checkInMethod("MANUAL")
                .build();

        when(eventRepository.findById(event.getEventId())).thenReturn(Optional.of(event));
        when(attendanceRepository.findByEventIdAndTenantId(event.getEventId(), tenantId)).thenReturn(List.of());
        when(attendanceRepository.save(any(EventAttendance.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        EventAttendance result = eventAttendanceService.checkIn(request, actorUserId);

        assertThat(result.getUser()).isNull();
        assertThat(result.getGuestFullName()).isEqualTo("Guest Visitor");
        assertThat(result.getGuestEmail()).isEqualTo("guest@example.com");
        assertThat(result.getGuestPhone()).isEqualTo("555-0100");
        assertThat(result.getStatus()).isEqualTo(AttendanceStatus.CHECKED_IN);
        assertThat(result.getCheckedInBy()).isEqualTo(actorUserId);
    }

    @Test
    void checkIn_whenGuestRequestHasNoStableIdentifier_throwsIllegalArgument() {
        CheckInRequestDTO request = CheckInRequestDTO.builder()
                .eventId(event.getEventId())
                .guestFullName("Guest Visitor")
                .checkInMethod("MANUAL")
                .build();

        when(eventRepository.findById(event.getEventId())).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> eventAttendanceService.checkIn(request, actorUserId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("guestEmail or guestPhone is required");

        verify(attendanceRepository, never()).save(any());
    }

    @Test
    void getAttendanceReportByUser_whenUserHasNoMembership_throwsIllegalArgument() {
        UserEntity nonMemberUser = UserEntity.builder()
                .uuid(userId)
                .affiliatedTenantId(tenantId)
                .build();
        when(userRepository.findByUuidAndAffiliatedTenantId(userId, tenantId)).thenReturn(Optional.of(nonMemberUser));

        assertThatThrownBy(() -> eventAttendanceService.getAttendanceReportByUser(userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("member-linked user");
    }

    @Test
    void getAttendanceReportByUser_whenMemberLinked_returnsDetailedRows() {
        EventAttendance attendance = EventAttendance.builder()
                .id(9L)
                .event(event)
                .user(user)
                .status(AttendanceStatus.CHECKED_IN)
                .checkInMethod("QR")
                .build();
        when(userRepository.findByUuidAndAffiliatedTenantId(userId, tenantId)).thenReturn(Optional.of(user));
        when(attendanceRepository.findDetailedByUserUuidAndTenantId(userId, tenantId)).thenReturn(List.of(attendance));

        List<MemberAttendanceReportResponse> result = eventAttendanceService.getAttendanceReportByUser(userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).eventTitle()).isEqualTo(event.getTitle());
        assertThat(result.get(0).status()).isEqualTo(AttendanceStatus.CHECKED_IN);
    }

    @Test
    void toResponse_whenGuestAttendance_mapsGuestFields() {
        EventAttendance attendance = EventAttendance.builder()
                .id(22L)
                .event(event)
                .guestFullName("Guest Visitor")
                .guestEmail("guest@example.com")
                .guestPhone("555-0100")
                .status(AttendanceStatus.ABSENT)
                .build();

        EventAttendanceResponse response = eventAttendanceService.toResponse(attendance);

        assertThat(response.userId()).isNull();
        assertThat(response.attendeeType()).isEqualTo(AttendanceAttendeeType.GUEST);
        assertThat(response.attendeeName()).isEqualTo("Guest Visitor");
        assertThat(response.guestEmail()).isEqualTo("guest@example.com");
        assertThat(response.guestPhone()).isEqualTo("555-0100");
    }
}
