package com.anastasia.Anastasia_BackEnd.UnitTests.service.appointments;

import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.modules.appointments.dto.AppointmentAssigneeRequest;
import com.anastasia.Anastasia_BackEnd.modules.appointments.dto.AppointmentCreateRequest;
import com.anastasia.Anastasia_BackEnd.modules.appointments.dto.AppointmentParticipantRequest;
import com.anastasia.Anastasia_BackEnd.modules.appointments.dto.AppointmentRescheduleRequest;
import com.anastasia.Anastasia_BackEnd.modules.appointments.dto.AppointmentStatusUpdateRequest;
import com.anastasia.Anastasia_BackEnd.modules.appointments.dto.MemberAppointmentResponse;
import com.anastasia.Anastasia_BackEnd.modules.appointments.mappers.AppointmentMapper;
import com.anastasia.Anastasia_BackEnd.modules.appointments.model.AppointmentAssignmentEntity;
import com.anastasia.Anastasia_BackEnd.modules.appointments.model.AppointmentEntity;
import com.anastasia.Anastasia_BackEnd.modules.appointments.model.AppointmentSource;
import com.anastasia.Anastasia_BackEnd.modules.appointments.model.AppointmentStatus;
import com.anastasia.Anastasia_BackEnd.modules.appointments.model.AppointmentType;
import com.anastasia.Anastasia_BackEnd.modules.appointments.model.AssignedRole;
import com.anastasia.Anastasia_BackEnd.modules.appointments.model.ContactPreference;
import com.anastasia.Anastasia_BackEnd.modules.appointments.model.LocationType;
import com.anastasia.Anastasia_BackEnd.modules.appointments.repository.AppointmentRepository;
import com.anastasia.Anastasia_BackEnd.modules.appointments.service.AppointmentServiceImpl;
import com.anastasia.Anastasia_BackEnd.modules.calendar.model.CalendarVisibility;
import com.anastasia.Anastasia_BackEnd.modules.calendar.model.CalendarEntryEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.Adult_MemberEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child.Child_MemberEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import com.anastasia.Anastasia_BackEnd.UnitTests.support.LenientMockitoTest;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Behaviors to protect:
 * - Appointment lifecycle validators (date ordering, contact preference requirements) must reject bad payloads before any persistence.
 * - Reschedule/update operations must respect tenant scoping and raise when conflicts exist or when a user is outside the tenant.
 * - Status transitions must keep cancellation/confirmation timestamps consistent and propagate trimmed reasons.
 * <p>
 * Edge cases: missing tenant context, null participants/assignments, overlapping appointments returned by the repository, and
 * user records belonging to another tenant.
 */
@LenientMockitoTest
@MockitoSettings(strictness = Strictness.LENIENT)
class AppointmentServiceUnitTest {

    @Mock private AppointmentRepository appointmentRepository;
    @Mock private AppointmentMapper appointmentMapper;
    @Mock private com.anastasia.Anastasia_BackEnd.modules.calendar.service.CalendarEntryService calendarEntryService;
    @Mock private com.anastasia.Anastasia_BackEnd.modules.calendar.repository.CalendarEntryRepository calendarEntryRepository;
    @Mock private com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChurchRepository churchRepository;
    @Mock private com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository userRepository;
    @Mock private com.anastasia.Anastasia_BackEnd.modules.registration.repository.MemberRepository memberRepository;
    @Mock private com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChildRepository childRepository;

    @InjectMocks private AppointmentServiceImpl appointmentService;

    private UUID tenantId;
    private ChurchEntity church;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        church = new ChurchEntity();
        church.setTenant(new TenantEntity());
        church.getTenant().setId(tenantId);
        when(churchRepository.findByTenantId(tenantId)).thenReturn(Optional.of(church));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        when(churchRepository.findByTenantId(any())).thenReturn(Optional.empty());
    }

    @Test
    void createAppointment_shouldRejectEndBeforeStart() {
        AppointmentCreateRequest request = new AppointmentCreateRequest(
                "one",
                null,
                AppointmentType.BAPTISM_PREP,
                Instant.parse("2026-01-01T10:00:00Z"),
                Instant.parse("2026-01-01T09:00:00Z"),
                "UTC",
                LocationType.ONSITE,
                "hall",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                Set.of(),
                Set.of()
        );

        assertThatThrownBy(() -> appointmentService.createAppointment(request, UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("endDateTime must be after startDateTime");
    }

    @Test
    void createAppointment_shouldRejectMissingPhoneWhenPhonePreferenceIsForced() {
        AppointmentCreateRequest request = new AppointmentCreateRequest(
                "one",
                null,
                AppointmentType.CONFESSION,
                Instant.parse("2026-01-01T10:00:00Z"),
                Instant.parse("2026-01-01T11:00:00Z"),
                "UTC",
                LocationType.ONSITE,
                "hall",
                null,
                null,
                null,
                null,
                "owner@example.com",
                ContactPreference.PHONE,
                null,
                null,
                Set.of(),
                Set.of()
        );

        assertThatThrownBy(() -> appointmentService.createAppointment(request, UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("contactPhone is required when contactPreference is PHONE");
    }

    @Test
    void rescheduleAppointment_shouldRejectWhenAssigneeConflicts() {
        TenantContext.setTenantId(tenantId);
        UUID appointmentId = UUID.randomUUID();
        AppointmentEntity appointment = createMinimalAppointment(appointmentId);
        AppointmentAssignmentEntity assignment = AppointmentAssignmentEntity.builder()
                .appointment(appointment)
                .userId(UUID.randomUUID())
                .role(AssignedRole.STAFF)
                .build();
        appointment.getAssignments().add(assignment);

        when(appointmentRepository.findByIdAndTenantId(appointmentId, tenantId)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.findConflicts(eq(tenantId), any(), any(), any(), eq(appointmentId), any()))
                .thenReturn(List.of(createMinimalAppointment(UUID.randomUUID())));

        AppointmentRescheduleRequest request = new AppointmentRescheduleRequest(
                Instant.now().plusSeconds(7200),
                Instant.now().plusSeconds(10800),
                "need new time"
        );

        assertThatThrownBy(() -> appointmentService.rescheduleAppointment(appointmentId, request, UUID.randomUUID()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("conflicting appointment");
    }

    @Test
    void updateStatus_shouldPopulateCancellationMeta() {
        TenantContext.setTenantId(tenantId);
        UUID appointmentId = UUID.randomUUID();
        AppointmentEntity appointment = createMinimalAppointment(appointmentId);
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        when(appointmentRepository.findByIdAndTenantId(appointmentId, tenantId)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any(AppointmentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(appointmentMapper.toResponse(any())).thenAnswer(invocation -> buildResponse(invocation.getArgument(0)));

        AppointmentStatusUpdateRequest request = new AppointmentStatusUpdateRequest(AppointmentStatus.CANCELLED, "  patient no show  ");

        appointmentService.updateStatus(appointmentId, request, UUID.randomUUID());

        ArgumentCaptor<AppointmentEntity> captor = ArgumentCaptor.forClass(AppointmentEntity.class);
        verify(appointmentRepository).save(captor.capture());
        AppointmentEntity saved = captor.getValue();

        assertThat(saved.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(saved.getCanceledAt()).isNotNull();
        assertThat(saved.getCancellationReason()).isEqualTo("patient no show");
        assertThat(saved.getConfirmedAt()).isEqualTo(saved.getCanceledAt());
    }

    @Test
    void createAppointment_shouldOverrideMemberControlledSource() {
        TenantContext.setTenantId(tenantId);
        UUID userId = UUID.randomUUID();
        UUID calendarEntryId = UUID.randomUUID();
        Adult_MemberEntity membership = Adult_MemberEntity.builder().id(77L).build();
        UserEntity user = new UserEntity();
        user.setUuid(userId);
        user.setTenantId(tenantId);
        user.setMembership(membership);

        AppointmentCreateRequest request = new AppointmentCreateRequest(
                "one",
                null,
                AppointmentType.BAPTISM_PREP,
                Instant.parse("2026-01-01T10:00:00Z"),
                Instant.parse("2026-01-01T11:00:00Z"),
                "UTC",
                LocationType.ONSITE,
                "hall",
                AppointmentStatus.REQUESTED,
                AppointmentSource.KIOSK,
                null,
                null,
                "owner@example.com",
                ContactPreference.EMAIL,
                null,
                null,
                Set.of(),
                Set.of()
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(calendarEntryService.createEntry(any(), eq(userId))).thenReturn(
                new com.anastasia.Anastasia_BackEnd.modules.calendar.dto.CalendarEntryResponse(
                        calendarEntryId,
                        null,
                        request.title(),
                        request.description(),
                        null,
                        request.startDateTime(),
                        request.endDateTime(),
                        request.timeZone(),
                        false,
                        CalendarVisibility.PRIEST_ONLY,
                        null,
                        null,
                        null,
                        Set.of(),
                        userId
                )
        );
        when(calendarEntryRepository.findById(calendarEntryId)).thenReturn(Optional.of(new CalendarEntryEntity()));
        when(appointmentRepository.save(any(AppointmentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(appointmentMapper.toResponse(any())).thenAnswer(invocation -> buildResponse(invocation.getArgument(0)));

        appointmentService.createAppointment(request, userId);

        ArgumentCaptor<AppointmentEntity> captor = ArgumentCaptor.forClass(AppointmentEntity.class);
        verify(appointmentRepository).save(captor.capture());
        assertThat(captor.getValue().getSource()).isEqualTo(AppointmentSource.REQUEST_MODULE);
    }

    @Test
    void listMyAppointments_shouldRejectUserOutsideTenantScope() {
        TenantContext.setTenantId(tenantId);
        UUID appointmentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UserEntity user = new UserEntity();
        user.setUuid(userId);
        user.setTenantId(UUID.randomUUID());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> appointmentService.getMyAppointment(appointmentId, userId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Authenticated user is not in tenant scope");

        verify(appointmentRepository, times(0)).findMemberVisibleByIdAndTenantId(any(), any(), any());
    }

    @Test
    void getMyAppointment_shouldResolveOnlyCurrentMemberVisibleIds() {
        TenantContext.setTenantId(tenantId);
        UUID appointmentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Adult_MemberEntity membership = Adult_MemberEntity.builder().id(77L).build();
        UserEntity user = new UserEntity();
        user.setUuid(userId);
        user.setTenantId(tenantId);
        user.setMembership(membership);
        Child_MemberEntity childInTenant = Child_MemberEntity.builder().id(88L).build();
        childInTenant.setTenantId(tenantId);
        Child_MemberEntity childOtherTenant = Child_MemberEntity.builder().id(99L).build();
        childOtherTenant.setTenantId(UUID.randomUUID());
        AppointmentEntity appointment = createMinimalAppointment(appointmentId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(childRepository.findByFatherIdOrMotherId(77L, 77L)).thenReturn(List.of(childInTenant, childOtherTenant));
        when(appointmentRepository.findMemberVisibleByIdAndTenantId(eq(appointmentId), eq(tenantId), any()))
                .thenReturn(Optional.of(appointment));
        when(appointmentMapper.toMemberResponse(appointment)).thenReturn(buildMemberResponse(appointment));

        appointmentService.getMyAppointment(appointmentId, userId);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Set<Long>> memberIds = ArgumentCaptor.forClass(Set.class);
        verify(appointmentRepository).findMemberVisibleByIdAndTenantId(eq(appointmentId), eq(tenantId), memberIds.capture());
        assertThat(memberIds.getValue()).containsExactlyInAnyOrder(77L, 88L);
    }

    @Test
    void getMyAppointment_shouldDenyWhenAppointmentIsNotVisibleToCurrentMember() {
        TenantContext.setTenantId(tenantId);
        UUID appointmentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Adult_MemberEntity membership = Adult_MemberEntity.builder().id(77L).build();
        UserEntity user = new UserEntity();
        user.setUuid(userId);
        user.setTenantId(tenantId);
        user.setMembership(membership);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(childRepository.findByFatherIdOrMotherId(77L, 77L)).thenReturn(List.of());
        when(appointmentRepository.findMemberVisibleByIdAndTenantId(eq(appointmentId), eq(tenantId), any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> appointmentService.getMyAppointment(appointmentId, userId))
                .isInstanceOf(jakarta.persistence.EntityNotFoundException.class)
                .hasMessageContaining("Appointment not found");
    }

    private AppointmentEntity createMinimalAppointment(UUID id) {
        return AppointmentEntity.builder()
                .id(id)
                .tenantId(tenantId)
                .church(church)
                .title("test")
                .type(AppointmentType.BAPTISM_PREP)
                .status(AppointmentStatus.REQUESTED)
                .source(AppointmentSource.MANUAL)
                .locationType(LocationType.ONSITE)
                .locationLabel("Hall")
                .startAtUtc(Instant.now())
                .timezone("UTC")
                .firstVisit(true)
                .sacramentRelated(true)
                .requestedAt(Instant.now())
                .build();
    }

    private com.anastasia.Anastasia_BackEnd.modules.appointments.dto.AppointmentResponse buildResponse(AppointmentEntity appointment) {
        return new com.anastasia.Anastasia_BackEnd.modules.appointments.dto.AppointmentResponse(
                appointment.getId(),
                appointment.getTitle(),
                appointment.getDescription(),
                appointment.getType(),
                appointment.getStartAtUtc(),
                appointment.getEndAtUtc(),
                appointment.getTimezone(),
                appointment.getLocationType(),
                appointment.getLocationLabel(),
                appointment.getStatus(),
                appointment.getSource(),
                null,
                null,
                Set.of(),
                Set.of(),
                appointment.getLinkedRequestId(),
                appointment.getNotesForMember(),
                appointment.getContactPhone(),
                appointment.getContactEmail(),
                appointment.getContactPreference(),
                appointment.isPrivateNotesExists(),
                appointment.isFirstVisit(),
                appointment.isSacramentRelated(),
                appointment.getRequestedAt(),
                appointment.getConfirmedAt(),
                appointment.getCompletedAt(),
                appointment.getCanceledAt(),
                appointment.getCancellationReason(),
                appointment.getOutcomeNotes(),
                Set.of()
        );
    }

    private MemberAppointmentResponse buildMemberResponse(AppointmentEntity appointment) {
        return new MemberAppointmentResponse(
                appointment.getId(),
                appointment.getTitle(),
                appointment.getDescription(),
                appointment.getType(),
                appointment.getStartAtUtc(),
                appointment.getEndAtUtc(),
                appointment.getTimezone(),
                appointment.getLocationType(),
                appointment.getLocationLabel(),
                appointment.getStatus(),
                appointment.getSource(),
                null,
                null,
                appointment.getNotesForMember(),
                appointment.getContactPhone(),
                appointment.getContactEmail(),
                appointment.getContactPreference(),
                appointment.isFirstVisit(),
                appointment.isSacramentRelated(),
                appointment.getConfirmedAt(),
                appointment.getCanceledAt()
        );
    }
}
