package com.anastasia.Anastasia_BackEnd.modules.dashboard.service;

import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.core.auth.principal.UserPrincipal;
import com.anastasia.Anastasia_BackEnd.modules.appointments.model.AppointmentEntity;
import com.anastasia.Anastasia_BackEnd.modules.appointments.model.AppointmentStatus;
import com.anastasia.Anastasia_BackEnd.modules.appointments.model.AppointmentType;
import com.anastasia.Anastasia_BackEnd.modules.appointments.model.AssignedRole;
import com.anastasia.Anastasia_BackEnd.modules.appointments.repository.AppointmentRepository;
import com.anastasia.Anastasia_BackEnd.modules.dashboard.dto.MemberOverviewItem;
import com.anastasia.Anastasia_BackEnd.modules.dashboard.dto.PriestDashboardResponse;
import com.anastasia.Anastasia_BackEnd.modules.dashboard.dto.PriestDashboardStats;
import com.anastasia.Anastasia_BackEnd.modules.dashboard.dto.PriestRequestItem;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.Adult_MemberEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.MemberStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child.Child_MemberEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.priest.PriestEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChildRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.MemberRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.PriestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class PriestDashboardService {

    private static final int RECENT_LIMIT = 5;

    private final MemberRepository memberRepository;
    private final ChildRepository childRepository;
    private final PriestRepository priestRepository;
    private final AppointmentRepository appointmentRepository;

    public PriestDashboardResponse getSummary() {
        UUID tenantId = requireTenantId();
        UUID userId = requireCurrentUserId();
        String priestNumber = resolvePriestNumber(userId);

        long adultsCount = 0;
        long childrenCount = 0;
        List<MemberOverviewItem> recentMembers = List.of();
        if (priestNumber != null && !priestNumber.isBlank()) {
            adultsCount = memberRepository.countByTenantIdAndPriestNumberAndStatusValueNot(
                    tenantId, priestNumber, MemberStatus.PENDING.name()
            );
            childrenCount = childRepository.countByTenantIdAndPriestNumberAndStatusValueNot(
                    tenantId, priestNumber, MemberStatus.PENDING.name()
            );
            recentMembers = buildRecentMembers(tenantId, priestNumber);
        }

        List<PriestRequestItem> recentRequests = buildRecentRequests(tenantId, userId);

        PriestDashboardStats stats = PriestDashboardStats.builder()
                .activeMembers(adultsCount + childrenCount)
                .children(childrenCount)
                .families(adultsCount)
                .build();

        return PriestDashboardResponse.builder()
                .stats(stats)
                .recentMembers(recentMembers)
                .recentRequests(recentRequests)
                .build();
    }

    private List<MemberOverviewItem> buildRecentMembers(UUID tenantId, String priestNumber) {
        var page = PageRequest.of(0, RECENT_LIMIT, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<MemberOverviewItem> adults = memberRepository.findByTenantIdAndPriestNumber(tenantId, priestNumber, page)
                .map(this::toMemberOverview)
                .getContent();
        List<MemberOverviewItem> children = childRepository.findByTenantIdAndPriestNumber(tenantId, priestNumber, page)
                .map(this::toMemberOverview)
                .getContent();

        return Stream.concat(adults.stream(), children.stream())
                .sorted(Comparator.comparing(MemberOverviewItem::getRegisteredAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(RECENT_LIMIT)
                .toList();
    }

    private List<PriestRequestItem> buildRecentRequests(UUID tenantId, UUID userId) {
        return appointmentRepository.findByTenantId(tenantId).stream()
                .filter(appointment -> isAssignedToPriest(appointment, userId))
                .sorted(Comparator.comparing(this::resolveAppointmentDate, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(RECENT_LIMIT)
                .map(this::toRequestItem)
                .toList();
    }

    private boolean isAssignedToPriest(AppointmentEntity appointment, UUID userId) {
        return appointment.getAssignments().stream().anyMatch(assignment ->
                userId.equals(assignment.getUserId())
                        && (assignment.getRole() == null || assignment.getRole() == AssignedRole.PRIEST)
        );
    }

    private PriestRequestItem toRequestItem(AppointmentEntity appointment) {
        String memberName = appointment.getParticipants().stream()
                .map(participant -> participant.getFullName())
                .filter(name -> name != null && !name.isBlank())
                .findFirst()
                .orElse(appointment.getTitle());

        return PriestRequestItem.builder()
                .category(mapRequestCategory(appointment.getType()))
                .name(memberName)
                .status(mapRequestStatus(appointment.getStatus()))
                .date(resolveAppointmentDate(appointment))
                .build();
    }

    private Instant resolveAppointmentDate(AppointmentEntity appointment) {
        if (appointment.getStartAtUtc() != null) {
            return appointment.getStartAtUtc();
        }
        if (appointment.getCreatedAt() != null) {
            return appointment.getCreatedAt();
        }
        return null;
    }

    private String mapRequestCategory(AppointmentType type) {
        if (type == null) {
            return "CALL";
        }
        return switch (type) {
            case CONFESSION -> "CONFESSION";
            case SICK_VISIT, HOUSE_BLESSING -> "VISIT";
            default -> "CALL";
        };
    }

    private String mapRequestStatus(AppointmentStatus status) {
        if (status == null) {
            return "PENDING";
        }
        return switch (status) {
            case COMPLETED -> "COMPLETED";
            case CANCELLED, NO_SHOW -> "ABORTED";
            default -> "PENDING";
        };
    }

    private MemberOverviewItem toMemberOverview(Adult_MemberEntity member) {
        return MemberOverviewItem.builder()
                .id(member.getId())
                .name(fullName(member.getFirstName(), member.getFatherName(), member.getGrandFatherName()))
                .type("ADULT")
                .status(member.getStatus())
                .registeredAt(member.getCreatedAt())
                .build();
    }

    private MemberOverviewItem toMemberOverview(Child_MemberEntity member) {
        return MemberOverviewItem.builder()
                .id(member.getId())
                .name(fullName(member.getFirstName(), member.getFatherName(), member.getGrandFatherName()))
                .type("CHILD")
                .status(member.getStatus())
                .registeredAt(member.getCreatedAt())
                .build();
    }

    private String fullName(String first, String father, String grandFather) {
        return Stream.of(first, father, grandFather)
                .filter(value -> value != null && !value.isBlank())
                .reduce((a, b) -> a + " " + b)
                .orElse("");
    }

    private String resolvePriestNumber(UUID userId) {
        return priestRepository.findByUser_Uuid(userId)
                .map(PriestEntity::getPriestNumber)
                .orElse(null);
    }

    private UUID requireTenantId() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant context is missing for dashboard data");
        }
        return tenantId;
    }

    private UUID requireCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getUserUuid();
        }
        throw new IllegalStateException("Authenticated user context is missing for priest dashboard");
    }
}
