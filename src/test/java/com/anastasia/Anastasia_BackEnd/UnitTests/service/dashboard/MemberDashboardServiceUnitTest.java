package com.anastasia.Anastasia_BackEnd.UnitTests.service.dashboard;

import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.core.auth.principal.UserPrincipal;
import com.anastasia.Anastasia_BackEnd.modules.appointments.model.AppointmentEntity;
import com.anastasia.Anastasia_BackEnd.modules.appointments.model.AppointmentParticipantEntity;
import com.anastasia.Anastasia_BackEnd.modules.appointments.model.AppointmentStatus;
import com.anastasia.Anastasia_BackEnd.modules.appointments.model.ParticipantRole;
import com.anastasia.Anastasia_BackEnd.modules.events.model.EventEntity;
import com.anastasia.Anastasia_BackEnd.modules.events.model.EventType;
import com.anastasia.Anastasia_BackEnd.modules.events.model.attendance.EventAttendance;
import com.anastasia.Anastasia_BackEnd.modules.events.repository.EventAttendanceRepository;
import com.anastasia.Anastasia_BackEnd.modules.events.repository.EventRepository;
import com.anastasia.Anastasia_BackEnd.modules.payments.domain.model.PaymentIntent;
import com.anastasia.Anastasia_BackEnd.modules.payments.domain.model.PaymentPurpose;
import com.anastasia.Anastasia_BackEnd.modules.payments.domain.model.PaymentStatus;
import com.anastasia.Anastasia_BackEnd.modules.payments.repository.PaymentIntentRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.Adult_MemberEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child.Child_MemberEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.MemberLifecycleStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChildRepository;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.modules.dashboard.service.MemberDashboardService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import com.anastasia.Anastasia_BackEnd.UnitTests.support.LenientMockitoTest;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Behaviors:
 * - Member dashboards must respect the tenant of the authenticated user, report family counts, upcoming events, and sacrament tallies.
 * - Upcoming status should distinguish between registered vs. open events and compute donation sums with tenant/user scope.
 * Edge cases: missing payment providers, empty event or appointment feeds, and authentication state lacking membership linkage.
 */
@LenientMockitoTest
@Tag("experimental")
class MemberDashboardServiceUnitTest {

    @Mock private com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository userRepository;
    @Mock private ChildRepository childRepository;
    @Mock private EventRepository eventRepository;
    @Mock private EventAttendanceRepository eventAttendanceRepository;
    @Mock private com.anastasia.Anastasia_BackEnd.modules.appointments.repository.AppointmentRepository appointmentRepository;
    @Mock private ObjectProvider<PaymentIntentRepository> paymentIntentRepositoryProvider;
    @Mock private PaymentIntentRepository paymentIntentRepository;

    @InjectMocks private MemberDashboardService memberDashboardService;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID userUuid = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void getSummary_shouldDescribeFamilyAndUpcomingEventsWithZeroDonationsWhenProviderMissing() {
        Adult_MemberEntity membership = createMember(100L, "Self");
        UserEntity user = createUser(membership);
        setAuthentication(user);
        when(userRepository.findById(userUuid)).thenReturn(Optional.of(user));

        when(childRepository.findFamilyChildren(eq(tenantId), eq(membership.getId()))).thenReturn(List.of(createChild(201L, "Kid")));
        when(appointmentRepository.findByTenantId(eq(tenantId))).thenReturn(List.of(createSacramentAppointment(membership.getId())));

        EventEntity mass = createEvent(300L, Instant.now().plusSeconds(60 * 60));
        when(eventRepository.findVisibleForUser(eq(tenantId), eq(userUuid), eq(user.getEmail()))).thenReturn(List.of(mass));
        EventAttendance attendance = EventAttendance.builder()
                .eventId(mass.getEventId())
                .userId(userUuid)
                .build();
        when(eventAttendanceRepository.findByUserUuid(eq(userUuid))).thenReturn(List.of(attendance));
        when(paymentIntentRepositoryProvider.getIfAvailable()).thenReturn(null);

        var response = memberDashboardService.getSummary();

        assertThat(response.getStats().getFamilyMembers()).isEqualTo(2);
        assertThat(response.getStats().getSacramentsCompleted()).isEqualTo(1);
        assertThat(response.getStats().getUpcomingEvents()).isEqualTo(1);
        assertThat(response.getStats().getMonthlyDonations().getAmount()).isZero();
        assertThat(response.getStats().getMonthlyDonations().getCurrency()).isEqualTo("USD");
        assertThat(response.getUpcomingEvents()).hasSize(1);
        assertThat(response.getUpcomingEvents().get(0).getStatus()).isEqualTo("REGISTERED");
        assertThat(response.getUpcomingEvents().get(0).getType()).isEqualTo("MASS");
    }

    @Test
    void getSummary_shouldReportRegisteredDonationsWhenProviderAvailable() {
        Adult_MemberEntity membership = createMember(101L, "Self");
        UserEntity user = createUser(membership);
        setAuthentication(user);
        when(userRepository.findById(userUuid)).thenReturn(Optional.of(user));

        when(childRepository.findFamilyChildren(eq(tenantId), eq(membership.getId()))).thenReturn(List.of());
        when(appointmentRepository.findByTenantId(eq(tenantId))).thenReturn(List.of());
        when(eventRepository.findVisibleForUser(eq(tenantId), eq(userUuid), eq(user.getEmail()))).thenReturn(List.of());
        when(paymentIntentRepositoryProvider.getIfAvailable()).thenReturn(paymentIntentRepository);

        when(paymentIntentRepository.sumCapturedAmountByTenantAndUserAndCapturedAtBetween(any(), any(), any(), any())).thenReturn(2200L);
        PaymentIntent payment = PaymentIntent.newInitiated(tenantId, PaymentPurpose.SPECIAL_EVENT_PAYMENT, 2200, "USD", 1L, userUuid, user.getEmail(), null, "key");
        payment.setCapturedGrossAmountMinor(2200L);
        payment.setCapturedCurrency("INR");
        payment.setStatus(PaymentStatus.CAPTURED);
        when(paymentIntentRepository.findTopByTenantIdAndUserIdAndStatusOrderByCapturedAtDesc(any(), any(), eq(PaymentStatus.CAPTURED)))
                .thenReturn(Optional.of(payment));

        var response = memberDashboardService.getSummary();

        assertThat(response.getStats().getMonthlyDonations().getAmount()).isEqualTo(22.0);
        assertThat(response.getStats().getMonthlyDonations().getCurrency()).isEqualTo("INR");
    }

    private Adult_MemberEntity createMember(Long id, String suffix) {
        Adult_MemberEntity adult = new Adult_MemberEntity();
        adult.setId(id);
        adult.setTenantId(tenantId);
        adult.setFirstName("First" + suffix);
        adult.setFatherName("Father" + suffix);
        adult.setGrandFatherName("Grand" + suffix);
        adult.setMotherName("Mom" + suffix);
        adult.setMothersFather("MomFather" + suffix);
        adult.setFirstNameLocal("Local" + suffix);
        adult.setFatherNameLocal("Local" + suffix);
        adult.setGrandFatherNameLocal("Local" + suffix);
        adult.setMotherFullNameLocal("Local" + suffix);
        adult.setChurchNumber("001");
        adult.setGenderValue(com.anastasia.Anastasia_BackEnd.modules.registration.model.member.MemberGender.MALE);
        adult.setBirthday(LocalDateTime.now().toLocalDate());
        adult.setStatusEnum(MemberLifecycleStatus.ACTIVE);
        return adult;
    }

    private Child_MemberEntity createChild(Long id, String suffix) {
        Child_MemberEntity child = new Child_MemberEntity();
        child.setId(id);
        child.setTenantId(tenantId);
        child.setFirstName("Child" + suffix);
        child.setFatherName("Father" + suffix);
        child.setGrandFatherName("Grand" + suffix);
        child.setMotherName("Mom" + suffix);
        child.setMotherFullNameLocal("Local" + suffix);
        child.setMothersFather("MotherFather" + suffix);
        child.setFirstNameLocal("Local" + suffix);
        child.setFatherNameLocal("Local" + suffix);
        child.setGrandFatherNameLocal("Local" + suffix);
        child.setChurchNumber("001");
        child.setGenderValue(com.anastasia.Anastasia_BackEnd.modules.registration.model.member.MemberGender.FEMALE);
        child.setStatusEnum(MemberLifecycleStatus.ACTIVE);
        return child;
    }

    private AppointmentEntity createSacramentAppointment(Long memberId) {
        AppointmentEntity appointment = new AppointmentEntity();
        appointment.setTenantId(tenantId);
        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointment.setSacramentRelated(true);
        AppointmentParticipantEntity participant = AppointmentParticipantEntity.builder()
                .memberId(memberId)
                .role(ParticipantRole.MEMBER)
                .build();
        appointment.setParticipants(Set.of(participant));
        return appointment;
    }

    private EventEntity createEvent(Long id, Instant startAt) {
        EventEntity event = new EventEntity();
        event.setEventId(id);
        event.setTenantId(tenantId);
        event.setStartAt(startAt);
        event.setTimezone(ZoneId.of("UTC").getId());
        event.setType(EventType.LITURGY);
        event.setTitle("Morning Mass");
        return event;
    }

    private void setAuthentication(UserEntity user) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(new UserPrincipal(user));
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }

    private UserEntity createUser(Adult_MemberEntity membership) {
        UserEntity user = new UserEntity();
        user.setUuid(userUuid);
        user.setEmail("member@example.com");
        user.setTenantId(tenantId);
        user.setMembership(membership);
        return user;
    }
}
