package com.anastasia.Anastasia_BackEnd.UnitTests.controller;

import com.anastasia.Anastasia_BackEnd.UnitTests.support.LenientMockitoTest;
import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.core.auth.principal.UserPrincipal;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import com.anastasia.Anastasia_BackEnd.core.notification.dto.NotificationInboxItemResponse;
import com.anastasia.Anastasia_BackEnd.core.notification.dto.NotificationInboxPageResponse;
import com.anastasia.Anastasia_BackEnd.core.notification.dto.NotificationPreferencesResponse;
import com.anastasia.Anastasia_BackEnd.core.notification.service.NotificationInboxService;
import com.anastasia.Anastasia_BackEnd.modules.dashboard.dto.*;
import com.anastasia.Anastasia_BackEnd.modules.dashboard.service.MemberDashboardService;
import com.anastasia.Anastasia_BackEnd.modules.dashboard.service.PriestDashboardService;
import com.anastasia.Anastasia_BackEnd.modules.dashboard.service.TenantAdminDashboardService;
import com.anastasia.Anastasia_BackEnd.modules.events.model.EventDTO;
import com.anastasia.Anastasia_BackEnd.modules.events.model.EventStatus;
import com.anastasia.Anastasia_BackEnd.modules.events.model.EventVisibilityType;
import com.anastasia.Anastasia_BackEnd.modules.events.model.EventType;
import com.anastasia.Anastasia_BackEnd.modules.events.model.attendance.AttendanceStatus;
import com.anastasia.Anastasia_BackEnd.modules.events.model.attendance.EventAttendance;
import com.anastasia.Anastasia_BackEnd.modules.events.model.attendance.EventAttendanceResponse;
import com.anastasia.Anastasia_BackEnd.modules.events.service.EventAttendanceService;
import com.anastasia.Anastasia_BackEnd.modules.events.service.EventService;
import com.anastasia.Anastasia_BackEnd.modules.mobile.controller.MobileController;
import com.anastasia.Anastasia_BackEnd.modules.mobile.dto.MobileCheckInRequest;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.Adult_MemberEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.Adult_MemberResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.Adult_MemberSummaryResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.MemberGender;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantType;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.MemberService;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@LenientMockitoTest
class MobileControllerUnitTest {

    @Mock private UserRepository userRepository;
    @Mock private TenantRepository tenantRepository;
    @Mock private MemberService memberService;
    @Mock private MemberDashboardService memberDashboardService;
    @Mock private PriestDashboardService priestDashboardService;
    @Mock private TenantAdminDashboardService tenantAdminDashboardService;
    @Mock private EventService eventService;
    @Mock private EventAttendanceService eventAttendanceService;
    @Mock private NotificationInboxService notificationInboxService;

    @InjectMocks private MobileController controller;

    private UUID tenantId;
    private UUID userId;
    private TenantEntity tenant;
    private UserEntity user;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();
        tenant = TenantEntity.builder()
                .id(tenantId)
                .displayName("St. Mark Parish")
                .slug("st-mark")
                .status(TenantStatus.ACTIVE)
                .tenantType(TenantType.CHURCH)
                .phoneVerified(true)
                .defaultTimezone("UTC")
                .defaultLocale("en")
                .countryCode("US")
                .build();
        user = UserEntity.builder()
                .uuid(userId)
                .email("user@example.com")
                .fullName("Jane Doe")
                .status(UserStatus.ACTIVE)
                .phoneNumber("+15550000000")
                .build();
        user.assignAffiliatedTenant(tenant);
        when(userRepository.findById(userId)).thenReturn(java.util.Optional.of(user));
        when(tenantRepository.findById(tenantId)).thenReturn(java.util.Optional.of(tenant));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        new UserPrincipal(user, Set.of(), Set.of()),
                        "n/a",
                        List.of()
                )
        );
        TenantContext.setTenantId(tenantId);
    }

    @Test
    void session_returnsCompactSessionPayload() {
        var response = controller.session();

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().user().email()).isEqualTo("user@example.com");
        assertThat(response.getBody().tenant().id()).isEqualTo(tenantId);
        assertThat(response.getBody().session().authenticated()).isTrue();
    }

    @Test
    void dashboardSummary_usesMemberSummaryWhenNoElevatedPermissions() {
        Adult_MemberEntity membership = Adult_MemberEntity.builder()
                .id(11L)
                .firstName("Jane")
                .fatherName("Doe")
                .grandFatherName("Smith")
                .tenantId(tenantId)
                .churchNumber("CH-01")
                .motherName("Jane")
                .mothersFather("Doe")
                .firstNameLocal("Jane")
                .fatherNameLocal("Doe")
                .grandFatherNameLocal("Smith")
                .motherFullNameLocal("Jane Doe")
                .birthday(LocalDate.of(1990, 1, 1))
                .fatherOfConfession("CONFESSOR")
                .build();
        membership.setGenderEnum(MemberGender.FEMALE);
        membership.setStatus("ACTIVE");
        user.assignMembership(membership);
        when(memberDashboardService.getSummary()).thenReturn(
                com.anastasia.Anastasia_BackEnd.modules.dashboard.dto.MemberDashboardResponse.builder()
                        .churchDisplayName("St. Mark Parish")
                        .stats(MemberDashboardStats.builder()
                                .familyMembers(2)
                                .upcomingEvents(1)
                                .sacramentsCompleted(3)
                                .monthlyDonations(MonthlyOffering.builder().amount(25.0).currency("USD").build())
                                .build())
                        .upcomingEvents(List.of(
                                MemberUpcomingEventItem.builder().name("Sunday Service").status("UPCOMING").type("LITURGY").date(LocalDate.now()).build()
                        ))
                        .familyMembers(List.of())
                        .build()
        );

        var response = controller.dashboardSummary();

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().tenantId()).isEqualTo(tenantId);
        assertThat(response.getBody().stats()).hasSize(3);
    }

    @Test
    void searchMembers_returnsMobileListShape() {
        when(memberService.searchNonPendingSummary(any(), anyString(), anyString())).thenReturn(
                new PageImpl<>(List.of(
                        Adult_MemberSummaryResponse.builder()
                                .id(1L)
                                .membershipNumber("M-001")
                                .fullName("Jane Doe")
                                .status("ACTIVE")
                                .phone("+1555000")
                                .email("jane@example.com")
                                .createdAt(Instant.parse("2026-06-30T00:00:00Z"))
                                .build()
                ))
        );

        var response = controller.searchMembers("Jane", 0, 10, "en");

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().items()).hasSize(1);
        assertThat(response.getBody().items().get(0).displayName()).isEqualTo("Jane Doe");
    }

    @Test
    void notifications_mapsSystemAndTenantScopes() {
        when(notificationInboxService.listInbox(any(), any(), anyInt(), anyInt())).thenReturn(
                new NotificationInboxPageResponse(
                        List.of(
                                new NotificationInboxItemResponse(
                                        1L,
                                        "System notice",
                                        "System message",
                                        com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationType.NOTIFICATION,
                                        false,
                                        Instant.parse("2026-06-30T10:00:00Z"),
                                        null,
                                        null
                                ),
                                new NotificationInboxItemResponse(
                                        2L,
                                        "Tenant notice",
                                        "Tenant message",
                                        com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationType.EVENT_REMINDER,
                                        true,
                                        Instant.parse("2026-06-30T11:00:00Z"),
                                        Instant.parse("2026-06-30T11:05:00Z"),
                                        tenantId
                                )
                        ),
                        0,
                        20,
                        1,
                        2,
                        List.of(20, 50, 100, 200),
                        2
                )
        );

        var response = controller.notifications(null, null, 0, 20);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().items()).hasSize(2);
        assertThat(response.getBody().items().get(0).scope()).isEqualTo("SYSTEM");
        assertThat(response.getBody().items().get(1).scope()).isEqualTo("TENANT");
    }

    @Test
    void visibleEvents_returnsCompactEventItems() {
        when(eventService.getVisibleEventsForUser(userId)).thenReturn(List.of(
                EventDTO.builder()
                        .eventId(99L)
                        .title("Sunday Worship")
                        .description("Main service")
                        .status(EventStatus.SCHEDULED)
                        .type(EventType.LITURGY)
                        .visibility(EventVisibilityType.ALL)
                        .startAt(Instant.parse("2026-07-05T08:00:00Z"))
                        .endAt(Instant.parse("2026-07-05T10:00:00Z"))
                        .timezone("UTC")
                        .location("Sanctuary")
                        .allDay(false)
                        .image("https://cdn.example/event.png")
                        .build()
        ));

        var response = controller.visibleEvents();

        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).title()).isEqualTo("Sunday Worship");
    }

    @Test
    void checkIn_returnsMobileCheckInResponse() {
        EventAttendance attendance = EventAttendance.builder()
                .id(7L)
                .eventId(99L)
                .userId(userId)
                .status(AttendanceStatus.CHECKED_IN)
                .checkInMethod("MOBILE")
                .checkInTime(java.time.LocalDateTime.parse("2026-06-30T10:00:00"))
                .checkedInBy(userId)
                .build();
        when(eventAttendanceService.checkIn(any(), any())).thenReturn(attendance);
        when(eventAttendanceService.toResponse(attendance)).thenReturn(EventAttendanceResponse.builder()
                .id(7L)
                .eventId(99L)
                .userId(userId)
                .status(AttendanceStatus.CHECKED_IN)
                .checkInMethod("MOBILE")
                .checkInTime(java.time.LocalDateTime.parse("2026-06-30T10:00:00"))
                .checkedInBy(userId)
                .build());

        var response = controller.checkIn(new MobileCheckInRequest(99L, userId, null, null, null, "MOBILE"));

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().eventId()).isEqualTo(99L);
        assertThat(response.getBody().status()).isEqualTo("CHECKED_IN");
    }

    @Test
    void notifications_returnsCompactPageAndUnreadCount() {
        when(notificationInboxService.listInbox(any(), any(), anyInt(), anyInt())).thenReturn(
                new NotificationInboxPageResponse(
                        List.of(NotificationInboxItemResponse.class.cast(new NotificationInboxItemResponse(
                                1L,
                                "Attendance updated",
                                "Your attendance was recorded.",
                                com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationType.NOTIFICATION,
                                false,
                                Instant.parse("2026-06-30T09:00:00Z"),
                                null,
                                null
                        ))),
                        0,
                        20,
                        1,
                        1,
                        List.of(20, 50),
                        12
                )
        );
        when(notificationInboxService.unreadCount()).thenReturn(12L);
        when(notificationInboxService.getPreferences()).thenReturn(new NotificationPreferencesResponse(true, false, true, Set.of()));

        var page = controller.notifications(null, null, 0, 20);
        var unread = controller.unreadCount();
        var prefs = controller.notificationPreferences();

        assertThat(page.getBody()).isNotNull();
        assertThat(page.getBody().items()).hasSize(1);
        assertThat(unread.getBody().count()).isEqualTo(12L);
        assertThat(prefs.getBody()).containsEntry("emailEnabled", true);
    }
}
