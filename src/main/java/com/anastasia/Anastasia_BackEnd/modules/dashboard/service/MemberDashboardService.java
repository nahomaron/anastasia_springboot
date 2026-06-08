package com.anastasia.Anastasia_BackEnd.modules.dashboard.service;

import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.core.auth.principal.UserPrincipal;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import com.anastasia.Anastasia_BackEnd.modules.appointments.model.AppointmentEntity;
import com.anastasia.Anastasia_BackEnd.modules.appointments.model.AppointmentStatus;
import com.anastasia.Anastasia_BackEnd.modules.appointments.repository.AppointmentRepository;
import com.anastasia.Anastasia_BackEnd.modules.dashboard.dto.MemberDashboardResponse;
import com.anastasia.Anastasia_BackEnd.modules.dashboard.dto.MemberDashboardStats;
import com.anastasia.Anastasia_BackEnd.modules.dashboard.dto.MemberFamilyItem;
import com.anastasia.Anastasia_BackEnd.modules.dashboard.dto.MemberUpcomingEventItem;
import com.anastasia.Anastasia_BackEnd.modules.dashboard.dto.MonthlyOffering;
import com.anastasia.Anastasia_BackEnd.modules.events.model.EventEntity;
import com.anastasia.Anastasia_BackEnd.modules.events.model.EventType;
import com.anastasia.Anastasia_BackEnd.modules.events.model.attendance.EventAttendance;
import com.anastasia.Anastasia_BackEnd.modules.events.repository.EventAttendanceRepository;
import com.anastasia.Anastasia_BackEnd.modules.events.repository.EventRepository;
import com.anastasia.Anastasia_BackEnd.modules.payments.domain.model.PaymentIntent;
import com.anastasia.Anastasia_BackEnd.modules.payments.domain.model.PaymentStatus;
import com.anastasia.Anastasia_BackEnd.modules.payments.repository.PaymentIntentRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.Adult_MemberEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child.Child_MemberEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChildRepository;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MemberDashboardService {

    private static final int UPCOMING_LIMIT = 5;

    private final UserRepository userRepository;
    private final ChildRepository childRepository;
    private final EventRepository eventRepository;
    private final EventAttendanceRepository eventAttendanceRepository;
    private final AppointmentRepository appointmentRepository;
    private final ObjectProvider<PaymentIntentRepository> paymentIntentRepositoryProvider;

    public MemberDashboardResponse getSummary() {
        UUID tenantId = requireTenantId();
        UserEntity user = requireCurrentUser(tenantId);

        List<MemberFamilyItem> familyMembers = buildFamilyMembers(user, tenantId);
        List<MemberUpcomingEventItem> upcomingEvents = buildUpcomingEvents(user, tenantId);
        long totalSacraments = familyMembers.stream().mapToLong(MemberFamilyItem::getSacramentsCompleted).sum();

        MemberDashboardStats stats = MemberDashboardStats.builder()
                .familyMembers(familyMembers.size())
                .upcomingEvents(upcomingEvents.size())
                .sacramentsCompleted(totalSacraments)
                .monthlyDonations(buildMonthlyDonations(tenantId, user.getUuid()))
                .build();

        return MemberDashboardResponse.builder()
                .stats(stats)
                .churchDisplayName(buildChurchDisplayName(user))
                .familyMembers(familyMembers)
                .upcomingEvents(upcomingEvents)
                .build();
    }

    private List<MemberFamilyItem> buildFamilyMembers(UserEntity user, UUID tenantId) {
        Adult_MemberEntity self = user.getMembership();
        if (self == null || self.getId() == null) {
            return List.of();
        }

        List<Child_MemberEntity> children = childRepository.findFamilyChildren(tenantId, self.getId());

        Map<Long, Long> completedSacramentsByMember = buildSacramentCountsByMember(tenantId);

        MemberFamilyItem selfItem = MemberFamilyItem.builder()
                .name(fullName(self.getFirstName(), self.getFatherName(), self.getGrandFatherName()))
                .relation("SELF")
                .status(mapMemberStatus(self.getStatus()))
                .sacramentsCompleted(completedSacramentsByMember.getOrDefault(self.getId(), 0L))
                .build();

        List<MemberFamilyItem> childItems = children.stream()
                .map(child -> MemberFamilyItem.builder()
                        .name(fullName(child.getFirstName(), child.getFatherName(), child.getGrandFatherName()))
                        .relation("CHILD")
                        .status(mapMemberStatus(child.getStatus()))
                        .sacramentsCompleted(completedSacramentsByMember.getOrDefault(child.getId(), 0L))
                        .build())
                .toList();

        return java.util.stream.Stream.concat(java.util.stream.Stream.of(selfItem), childItems.stream()).toList();
    }

    private Map<Long, Long> buildSacramentCountsByMember(UUID tenantId) {
        List<AppointmentEntity> appointments = appointmentRepository.findByTenantId(tenantId);
        Map<Long, Long> counts = new HashMap<>();
        for (AppointmentEntity appointment : appointments) {
            if (!appointment.isSacramentRelated() || appointment.getStatus() != AppointmentStatus.COMPLETED) {
                continue;
            }
            appointment.getParticipants().forEach(participant -> {
                if (participant.getMemberId() != null) {
                    counts.merge(participant.getMemberId(), 1L, Long::sum);
                }
            });
        }
        return counts;
    }

    private List<MemberUpcomingEventItem> buildUpcomingEvents(UserEntity user, UUID tenantId) {
        Map<Long, EventAttendance> attendanceByEventId = eventAttendanceRepository.findByUserUuidAndTenantId(user.getUuid(), tenantId).stream()
                .filter(attendance -> attendance.getEventId() != null)
                .collect(java.util.stream.Collectors.toMap(EventAttendance::getEventId, attendance -> attendance, (a, b) -> a));

        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        return eventRepository.findVisibleForUser(tenantId, user.getUuid(), user.getEmail()).stream()
                .filter(event -> event.getStartAt() != null && !toEventDate(event).isBefore(today))
                .sorted(Comparator.comparing(EventEntity::getStartAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(UPCOMING_LIMIT)
                .map(event -> MemberUpcomingEventItem.builder()
                        .name(event.getTitle())
                        .date(toEventDate(event))
                        .type(mapEventType(event))
                        .status(mapEventStatus(event, attendanceByEventId))
                        .build())
                .toList();
    }

    private String mapEventType(EventEntity event) {
        if (event == null) {
            return "EVENT";
        }
        if (event.getType() == EventType.LITURGY) {
            return "MASS";
        }
        String title = event.getTitle() == null ? "" : event.getTitle().toLowerCase(Locale.ROOT);
        if (title.contains("confession")) {
            return "CONFESSION";
        }
        return "EVENT";
    }

    private String mapEventStatus(EventEntity event, Map<Long, EventAttendance> attendanceByEventId) {
        EventAttendance attendance = attendanceByEventId.get(event.getEventId());
        if (attendance != null) {
            return "REGISTERED";
        }
        return "UPCOMING";
    }

    private LocalDate toEventDate(EventEntity event) {
        if (event == null || event.getStartAt() == null) {
            return LocalDate.MIN;
        }
        ZoneId zoneId = event.getTimezone() == null || event.getTimezone().isBlank()
                ? ZoneOffset.UTC
                : ZoneId.of(event.getTimezone());
        return event.getStartAt().atZone(zoneId).toLocalDate();
    }

    private MonthlyOffering buildMonthlyDonations(UUID tenantId, UUID userId) {
        PaymentIntentRepository paymentIntentRepository = paymentIntentRepositoryProvider.getIfAvailable();
        if (paymentIntentRepository == null) {
            return MonthlyOffering.builder()
                    .amount(0.0)
                    .currency("USD")
                    .build();
        }

        ZonedDateTime nowUtc = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime startOfMonth = nowUtc.withDayOfMonth(1).toLocalDate().atStartOfDay(ZoneOffset.UTC);
        Instant start = startOfMonth.toInstant();
        Instant end = startOfMonth.plusMonths(1).toInstant();

        long totalMinor = paymentIntentRepository.sumCapturedAmountByTenantAndUserAndCapturedAtBetween(tenantId, userId, start, end);
        String currency = paymentIntentRepository.findTopByTenantIdAndUserIdAndStatusOrderByCapturedAtDesc(tenantId, userId, PaymentStatus.CAPTURED)
                .map(this::resolveCurrency)
                .orElse("USD");

        return MonthlyOffering.builder()
                .amount(totalMinor / 100.0)
                .currency(currency)
                .build();
    }

    private String resolveCurrency(PaymentIntent payment) {
        if (payment.getCapturedCurrency() != null && !payment.getCapturedCurrency().isBlank()) {
            return payment.getCapturedCurrency();
        }
        if (payment.getAmount() != null && payment.getAmount().getCurrency() != null) {
            return payment.getAmount().getCurrency();
        }
        return "USD";
    }

    private String mapMemberStatus(String status) {
        if (status == null) {
            return "PENDING";
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "APPROVED", "ACTIVE" -> "ACTIVE";
            case "PENDING" -> "PENDING";
            default -> "PENDING";
        };
    }

    private String fullName(String first, String father, String grandFather) {
        return java.util.stream.Stream.of(first, father, grandFather)
                .filter(value -> value != null && !value.isBlank())
                .reduce((a, b) -> a + " " + b)
                .orElse("");
    }

    private String buildChurchDisplayName(UserEntity user) {
        ChurchEntity church = user.getTenant() != null ? user.getTenant().getChurch() : null;
        if (church == null) {
            return null;
        }

        return java.util.stream.Stream.of(
                        trimToNull(church.getPrefixLocal()),
                        trimToNull(church.getChurchNameLocal()),
                        trimToNull(church.getNeighborhoodLocal())
                )
                .filter(value -> value != null && !value.isBlank())
                .reduce((a, b) -> a + " " + b)
                .orElse(null);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private UUID requireTenantId() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant context is missing for dashboard data");
        }
        return tenantId;
    }

    private UserEntity requireCurrentUser(UUID tenantId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new IllegalStateException("Authenticated user context is missing for member dashboard");
        }
        return userRepository.findById(principal.getUserUuid())
                .filter(user -> tenantId.equals(user.getTenantId()))
                .orElseThrow(() -> new IllegalStateException("Authenticated user is not in tenant scope"));
    }
}
