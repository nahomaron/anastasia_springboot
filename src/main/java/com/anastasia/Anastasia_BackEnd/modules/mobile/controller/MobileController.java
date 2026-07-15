package com.anastasia.Anastasia_BackEnd.modules.mobile.controller;

import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.core.auth.principal.UserPrincipal;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import com.anastasia.Anastasia_BackEnd.core.notification.dto.NotificationInboxPageResponse;
import com.anastasia.Anastasia_BackEnd.core.notification.dto.UnreadCountResponse;
import com.anastasia.Anastasia_BackEnd.core.notification.dto.UpdateNotificationPreferencesRequest;
import com.anastasia.Anastasia_BackEnd.core.notification.service.NotificationInboxService;
import com.anastasia.Anastasia_BackEnd.modules.dashboard.dto.MemberDashboardResponse;
import com.anastasia.Anastasia_BackEnd.modules.dashboard.dto.MemberUpcomingEventItem;
import com.anastasia.Anastasia_BackEnd.modules.dashboard.dto.PriestDashboardResponse;
import com.anastasia.Anastasia_BackEnd.modules.dashboard.dto.TenantAdminDashboardResponse;
import com.anastasia.Anastasia_BackEnd.modules.dashboard.service.MemberDashboardService;
import com.anastasia.Anastasia_BackEnd.modules.dashboard.service.PriestDashboardService;
import com.anastasia.Anastasia_BackEnd.modules.dashboard.service.TenantAdminDashboardService;
import com.anastasia.Anastasia_BackEnd.modules.events.model.EventDTO;
import com.anastasia.Anastasia_BackEnd.modules.events.model.attendance.EventAttendanceResponse;
import com.anastasia.Anastasia_BackEnd.modules.events.service.EventAttendanceService;
import com.anastasia.Anastasia_BackEnd.modules.events.service.EventService;
import com.anastasia.Anastasia_BackEnd.modules.mobile.dto.*;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.Adult_MemberResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.Adult_MemberSummaryResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.MemberService;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/mobile")
@PreAuthorize("isAuthenticated()")
public class MobileController {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final MemberService memberService;
    private final MemberDashboardService memberDashboardService;
    private final PriestDashboardService priestDashboardService;
    private final TenantAdminDashboardService tenantAdminDashboardService;
    private final EventService eventService;
    private final EventAttendanceService eventAttendanceService;
    private final NotificationInboxService notificationInboxService;

    @GetMapping("/session")
    public ResponseEntity<MobileAuthResponse> session() {
        CurrentActor actor = requireActor();
        UserEntity user = loadUser(actor.userId());
        TenantEntity tenant = loadTenant(actor.tenantId());
        return ResponseEntity.ok(new MobileAuthResponse(
                null,
                null,
                "Bearer",
                null,
                toUserResponse(user),
                toTenantResponse(tenant),
                new MobileSessionResponse(
                        user.getUuid(),
                        user.getEmail(),
                        user.getFullName(),
                        actor.roles(),
                        actor.permissions(),
                        true,
                        user.isMustChangePassword(),
                        null
                )
        ));
    }

    @GetMapping("/tenant/current")
    public ResponseEntity<MobileTenantResponse> currentTenant() {
        return ResponseEntity.ok(toTenantResponse(loadTenant(requireActor().tenantId())));
    }

    @GetMapping("/dashboard/summary")
    public ResponseEntity<MobileDashboardSummaryResponse> dashboardSummary() {
        CurrentActor actor = requireActor();
        if (actor.hasAny("MANAGE_TENANTS", "VIEW_ALL_DATA", "MANAGE_USERS", "MANAGE_MEMBERS", "MANAGE_EVENTS", "MANAGE_FINANCE", "MANAGE_APPOINTMENT")) {
            TenantAdminDashboardResponse dashboard = tenantAdminDashboardService.getSummary();
            return ResponseEntity.ok(new MobileDashboardSummaryResponse(
                    primaryRole(actor.roles()),
                    actor.tenantId(),
                    tenantDisplayName(actor.tenantId()),
                    List.of(
                            new MobileStatItem("activeMembers", "Members", dashboard.getStats().getActiveMembers(), "neutral"),
                            new MobileStatItem("children", "Children", dashboard.getStats().getChildren(), "info"),
                            new MobileStatItem("priestsStaff", "Priests", dashboard.getStats().getPriestsStaff(), "success"),
                            new MobileStatItem("monthlyOffering", "Offering", Math.round(dashboard.getStats().getMonthlyOffering().getAmount()), "warning")
                    ),
                    List.of(
                            new MobileActionItem("searchMembers", "Search members", "search"),
                            new MobileActionItem("checkIn", "Check in", "scan"),
                            new MobileActionItem("events", "Events", "calendar")
                    ),
                    new MobileDashboardHighlights(
                            dashboard.getRecentMembers().stream().map(this::toMobileMemberOverview).toList(),
                            List.of(),
                            List.of()
                    )
            ));
        }

        if (actor.hasAny("VIEW_PRIEST_DASHBOARD", "MANAGE_MEMBERS", "VIEW_MEMBERS", "MANAGE_EVENTS", "VIEW_EVENTS")) {
            PriestDashboardResponse dashboard = priestDashboardService.getSummary();
            return ResponseEntity.ok(new MobileDashboardSummaryResponse(
                    primaryRole(actor.roles()),
                    actor.tenantId(),
                    tenantDisplayName(actor.tenantId()),
                    List.of(
                            new MobileStatItem("activeMembers", "Members", dashboard.getStats().getActiveMembers(), "neutral"),
                            new MobileStatItem("children", "Children", dashboard.getStats().getChildren(), "info"),
                            new MobileStatItem("families", "Families", dashboard.getStats().getFamilies(), "success")
                    ),
                    List.of(
                            new MobileActionItem("searchMembers", "Search members", "search"),
                            new MobileActionItem("events", "Events", "calendar")
                    ),
                    new MobileDashboardHighlights(
                            dashboard.getRecentMembers().stream().map(this::toMobileMemberOverview).toList(),
                            List.of(),
                            List.of()
                    )
            ));
        }

        MemberDashboardResponse dashboard = memberDashboardService.getSummary();
        return ResponseEntity.ok(new MobileDashboardSummaryResponse(
                primaryRole(actor.roles()),
                actor.tenantId(),
                dashboard.getChurchDisplayName(),
                List.of(
                        new MobileStatItem("familyMembers", "Family", dashboard.getStats().getFamilyMembers(), "neutral"),
                        new MobileStatItem("upcomingEvents", "Upcoming", dashboard.getStats().getUpcomingEvents(), "info"),
                        new MobileStatItem("sacramentsCompleted", "Sacraments", dashboard.getStats().getSacramentsCompleted(), "success")
                ),
                List.of(
                        new MobileActionItem("searchMembers", "Search members", "search"),
                        new MobileActionItem("events", "Events", "calendar"),
                        new MobileActionItem("notifications", "Notifications", "notifications")
                ),
                new MobileDashboardHighlights(
                        List.of(),
                        dashboard.getUpcomingEvents().stream().map(this::toMobileEventOverview).toList(),
                        List.of()
                )
        ));
    }

    @GetMapping("/members/search")
    public ResponseEntity<MobileMemberSearchResponse> searchMembers(
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "lang", required = false, defaultValue = "en") String language
    ) {
        Page<Adult_MemberSummaryResponse> results = memberService.searchNonPendingSummary(PageRequest.of(Math.max(page, 0), clampSize(size)), query, language);
        List<MobileMemberSummaryItem> items = results.getContent().stream().map(this::toMobileMemberSummary).toList();
        return ResponseEntity.ok(new MobileMemberSearchResponse(
                items,
                results.getNumber(),
                results.getSize(),
                results.getTotalPages(),
                results.getTotalElements(),
                results.hasNext()
        ));
    }

    @GetMapping("/members/{memberId}")
    public ResponseEntity<MobileMemberDetailResponse> getMember(@PathVariable Long memberId) {
        Adult_MemberResponse response = memberService.findMemberById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("Member not found"));
        return ResponseEntity.ok(toMobileMemberDetail(response));
    }

    @GetMapping("/events/visible")
    public ResponseEntity<List<MobileEventSummaryItem>> visibleEvents() {
        UUID userId = requireActor().userId();
        List<MobileEventSummaryItem> items = eventService.getVisibleEventsForUser(userId).stream().map(this::toMobileEventOverview).toList();
        return ResponseEntity.ok(items);
    }

    @GetMapping("/events/{eventId}")
    public ResponseEntity<MobileEventDetailResponse> getEvent(@PathVariable Long eventId) {
        UUID userId = requireActor().userId();
        EventDTO event = eventService.getEventByIdForUser(userId, eventId);
        return ResponseEntity.ok(toMobileEventDetail(event));
    }

    @PostMapping("/attendance/check-in")
    public ResponseEntity<MobileCheckInResponse> checkIn(@Valid @RequestBody MobileCheckInRequest request) {
        var attendance = eventAttendanceService.checkIn(
                new com.anastasia.Anastasia_BackEnd.modules.events.model.attendance.CheckInRequestDTO(
                        request.eventId(),
                        request.memberId(),
                        request.guestFullName(),
                        request.guestEmail(),
                        request.guestPhone(),
                        request.checkInMethod(),
                        null
                ),
                requireActor().userId()
        );
        EventAttendanceResponse response = eventAttendanceService.toResponse(attendance);
        return ResponseEntity.ok(new MobileCheckInResponse(
                response.id(),
                response.eventId(),
                response.userId(),
                response.attendeeType() != null ? response.attendeeType().name() : null,
                response.attendeeName(),
                response.status() != null ? response.status().name() : null,
                response.checkInMethod(),
                response.checkInTime(),
                false,
                "Check-in completed"
        ));
    }

    @GetMapping("/notifications")
    public ResponseEntity<MobileNotificationPageResponse> notifications(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        NotificationInboxPageResponse response = notificationInboxService.listInbox(status, type, page, size);
        return ResponseEntity.ok(new MobileNotificationPageResponse(
                response.items().stream().map(this::toMobileNotification).toList(),
                response.page(),
                response.size(),
                response.totalPages(),
                response.totalElements(),
                response.unreadCount()
        ));
    }

    @GetMapping("/notifications/unread-count")
    public ResponseEntity<MobileUnreadCountResponse> unreadCount() {
        UnreadCountResponse response = new UnreadCountResponse(notificationInboxService.unreadCount());
        return ResponseEntity.ok(new MobileUnreadCountResponse(response.unreadCount()));
    }

    @PatchMapping("/notifications/{notificationId}/read")
    public ResponseEntity<MobileNotificationItem> markNotificationRead(@PathVariable Long notificationId) {
        var response = notificationInboxService.markRead(notificationId);
        return ResponseEntity.ok(toMobileNotification(response));
    }

    @GetMapping("/notifications/preferences")
    public ResponseEntity<Map<String, Object>> notificationPreferences() {
        var response = notificationInboxService.getPreferences();
        return ResponseEntity.ok(Map.of(
                "emailEnabled", response.emailEnabled(),
                "smsEnabled", response.smsEnabled(),
                "inAppEnabled", response.inAppEnabled(),
                "mutedTypes", response.mutedTypes()
        ));
    }

    @PutMapping("/notifications/preferences")
    public ResponseEntity<Map<String, Object>> updateNotificationPreferences(@Valid @RequestBody UpdateNotificationPreferencesRequest request) {
        var response = notificationInboxService.updatePreferences(request);
        return ResponseEntity.ok(Map.of(
                "emailEnabled", response.emailEnabled(),
                "smsEnabled", response.smsEnabled(),
                "inAppEnabled", response.inAppEnabled(),
                "mutedTypes", response.mutedTypes()
        ));
    }

    private MobileMemberSummaryItem toMobileMemberSummary(Adult_MemberSummaryResponse member) {
        String displayName = firstNonBlank(member.getFullName(), member.getFullNameLocal(), member.getMembershipNumber());
        return new MobileMemberSummaryItem(
                member.getId(),
                member.getMembershipNumber(),
                displayName,
                null,
                member.getStatus(),
                member.getPhone(),
                member.getEmail(),
                null,
                member.getCreatedAt()
        );
    }

    private MobileMemberDetailResponse toMobileMemberDetail(Adult_MemberResponse member) {
        String displayName = firstNonBlank(fullName(member.getFirstName(), member.getFatherName(), member.getGrandFatherName()), member.getMembershipNumber());
        String avatarUrl = member.getAvatar() != null ? member.getAvatar().getImageUrl() : null;
        Map<String, Object> address = new HashMap<>();
        if (member.getAddress() != null) {
            address.put("addressLine1", member.getAddress().getAddressLine1());
            address.put("addressLine2", member.getAddress().getAddressLine2());
            address.put("city", member.getAddress().getCity());
            address.put("stateProvince", member.getAddress().getStateProvince());
            address.put("country", member.getAddress().getCountry());
            address.put("postalCode", member.getAddress().getPostalCode());
        }
        Map<String, Object> family = new HashMap<>();
        family.put("spouseMembershipNumber", member.getSpouseMembershipNumber());
        family.put("childrenCount", member.getNumberOfChildren());
        Map<String, Object> membership = new HashMap<>();
        membership.put("approvedAt", member.getApprovedAt());
        membership.put("registeredAt", member.getRegisteredAt());
        Map<String, Boolean> actions = new HashMap<>();
        actions.put("canCheckIn", Boolean.TRUE);
        actions.put("canCall", member.getPhone() != null && !member.getPhone().isBlank());
        actions.put("canMessage", member.getEmail() != null && !member.getEmail().isBlank());
        return new MobileMemberDetailResponse(
                member.getId(),
                member.getTenantId(),
                member.getMembershipNumber(),
                member.getChurchNumber(),
                displayName,
                member.getFirstNameLocal() != null || member.getFatherNameLocal() != null || member.getGrandFatherNameLocal() != null
                        ? fullName(member.getFirstNameLocal(), member.getFatherNameLocal(), member.getGrandFatherNameLocal())
                        : null,
                member.getStatus(),
                avatarUrl,
                member.getPhone(),
                member.getEmail(),
                member.getGender(),
                member.getBirthday(),
                address,
                family,
                membership,
                actions
        );
    }

    private MobileEventSummaryItem toMobileEventOverview(EventDTO event) {
        return new MobileEventSummaryItem(
                event.getEventId(),
                event.getTitle(),
                event.getDescription(),
                event.getStatus() != null ? event.getStatus().name() : null,
                event.getType() != null ? event.getType().name() : null,
                event.getStartAt(),
                event.getEndAt(),
                event.getTimezone(),
                event.getLocation(),
                event.isAllDay(),
                event.getImage(),
                true
        );
    }

    private MobileEventSummaryItem toMobileEventOverview(MemberUpcomingEventItem item) {
        return new MobileEventSummaryItem(
                null,
                item.getName(),
                null,
                item.getStatus(),
                item.getType(),
                null,
                null,
                null,
                null,
                false,
                null,
                true
        );
    }

    private MobileEventDetailResponse toMobileEventDetail(EventDTO event) {
        Map<String, Object> attendeeSummary = new HashMap<>();
        attendeeSummary.put("checkedIn", 0);
        attendeeSummary.put("expected", event.getCapacity() != null ? event.getCapacity() : 0);
        Map<String, Boolean> actions = new HashMap<>();
        actions.put("canCheckIn", Boolean.TRUE);
        actions.put("canViewAttendance", Boolean.FALSE);
        actions.put("canManage", Boolean.FALSE);
        return new MobileEventDetailResponse(
                event.getEventId(),
                event.getTitle(),
                event.getDescription(),
                event.getStatus() != null ? event.getStatus().name() : null,
                event.getType() != null ? event.getType().name() : null,
                event.getVisibility() != null ? event.getVisibility().name() : null,
                event.getStartAt(),
                event.getEndAt(),
                event.getTimezone(),
                event.getLocation(),
                event.isAllDay(),
                event.getImage(),
                event.getCapacity(),
                null,
                null,
                event.getRequiresRegistration(),
                event.getAllowGeoCheckIn(),
                attendeeSummary,
                actions
        );
    }

    private MobileNotificationItem toMobileNotification(com.anastasia.Anastasia_BackEnd.core.notification.dto.NotificationInboxItemResponse item) {
        return new MobileNotificationItem(
                item.id(),
                item.title(),
                item.message(),
                item.type() != null ? item.type().name() : null,
                item.tenantId() == null ? "SYSTEM" : "TENANT",
                item.read(),
                item.createdAt(),
                item.readAt()
        );
    }

    private MobileMemberSummaryItem toMobileMemberOverview(com.anastasia.Anastasia_BackEnd.modules.dashboard.dto.MemberOverviewItem item) {
        return new MobileMemberSummaryItem(
                item.getId(),
                null,
                item.getName(),
                null,
                item.getStatus(),
                null,
                null,
                null,
                item.getRegisteredAt()
        );
    }

    private MobileTenantResponse toTenantResponse(TenantEntity tenant) {
        return new MobileTenantResponse(
                tenant.getId(),
                tenant.getDisplayName(),
                tenant.getSlug(),
                tenant.getStatus(),
                tenant.getTenantType(),
                tenant.isPhoneVerified(),
                tenant.getDefaultTimezone(),
                tenant.getDefaultLocale(),
                tenant.getCountryCode()
        );
    }

    private String tenantDisplayName(UUID tenantId) {
        return tenantRepository.findById(tenantId)
                .map(TenantEntity::getDisplayName)
                .orElse(null);
    }

    private TenantEntity loadTenant(UUID tenantId) {
        if (tenantId == null) {
            throw new IllegalStateException("Tenant context is missing");
        }
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));
    }

    private UserEntity loadUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    private CurrentActor requireActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new IllegalStateException("Authenticated user context is missing");
        }
        UUID tenantId = principal.getTenantId() != null ? principal.getTenantId() : TenantContext.getTenantId();
        return new CurrentActor(
                principal.getUserUuid(),
                tenantId,
                principal.getAuthorities().stream().map(a -> a.getAuthority()).collect(java.util.stream.Collectors.toUnmodifiableSet()),
                principal.getRoleNames()
        );
    }

    private String primaryRole(Set<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return "MEMBER";
        }
        return roles.iterator().next();
    }

    private MobileUserResponse toUserResponse(UserEntity user) {
        return new MobileUserResponse(
                user.getUuid(),
                user.getFullName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getRoles().stream().map(role -> role.getRoleName()).collect(java.util.stream.Collectors.toUnmodifiableSet()),
                user.getRoles().stream()
                        .flatMap(role -> role.getPermissions().stream())
                        .map(permission -> permission.getName().name())
                        .collect(java.util.stream.Collectors.toUnmodifiableSet())
        );
    }

    private String fullName(String first, String father, String grandFather) {
        return java.util.stream.Stream.of(first, father, grandFather)
                .filter(value -> value != null && !value.isBlank())
                .reduce((a, b) -> a + " " + b)
                .orElse("");
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private int clampSize(int size) {
        return Math.min(Math.max(size, 1), 50);
    }

    private record CurrentActor(UUID userId, UUID tenantId, Set<String> permissions, Set<String> roles) {
        boolean hasAny(String... candidates) {
            for (String candidate : candidates) {
                if (permissions != null && permissions.contains(candidate)) {
                    return true;
                }
            }
            return false;
        }
    }
}
