package com.anastasia.Anastasia_BackEnd.modules.platform.admin.service;

import com.anastasia.Anastasia_BackEnd.modules.platform.admin.dto.PlatformPaymentRecordResponse;
import com.anastasia.Anastasia_BackEnd.modules.platform.admin.dto.PlatformPriestApplicationResponse;
import com.anastasia.Anastasia_BackEnd.modules.platform.admin.dto.PlatformSupportTicketResponse;
import com.anastasia.Anastasia_BackEnd.modules.platform.admin.dto.PlatformTenantRowResponse;
import com.anastasia.Anastasia_BackEnd.modules.platform.admin.dto.PlatformAdminSummaryResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.MemberLifecycleStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.priest.PriestEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.SubscriptionPlan;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.SubscriptionStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantSubscriptionEventType;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantSubscriptionEventEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChildRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.MemberRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.PriestRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantSubscriptionEventRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.onboarding.TenantPlanBillingCatalog;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlatformAdminReportService {

    private static final List<MemberLifecycleStatus> ACTIVE_MEMBER_STATUSES = List.of(
            MemberLifecycleStatus.APPROVED,
            MemberLifecycleStatus.ACTIVE
    );

    private static final List<TenantStatus> PENDING_TENANT_STATUSES = List.of(
            TenantStatus.DRAFT,
            TenantStatus.PENDING_VERIFICATION
    );

    private static final List<TenantSubscriptionEventType> PAYMENT_EVENT_TYPES = List.of(
            TenantSubscriptionEventType.PAYMENT_SUCCEEDED,
            TenantSubscriptionEventType.PAYMENT_FAILED,
            TenantSubscriptionEventType.PAYMENT_REFUNDED
    );

    private final TenantRepository tenantRepository;
    private final MemberRepository memberRepository;
    private final ChildRepository childRepository;
    private final PriestRepository priestRepository;
    private final TenantSubscriptionEventRepository eventRepository;
    private final TenantPlanBillingCatalog billingCatalog;

    public PlatformAdminSummaryResponse getSummary() {
        long totalTenants = tenantRepository.countByDeletedAtIsNull();
        long activeTenants = tenantRepository.countByStatus(TenantStatus.ACTIVE);
        long suspendedTenants = tenantRepository.countByStatus(TenantStatus.SUSPENDED);
        long pendingApprovals = tenantRepository.countByStatusIn(PENDING_TENANT_STATUSES);
        long activeMembers = memberRepository.countByStatusValueIn(ACTIVE_MEMBER_STATUSES)
                + childRepository.countByStatusValueIn(ACTIVE_MEMBER_STATUSES);
        long monthlyRevenue = tenantRepository.findAllWithSubscription().stream()
                .map(TenantEntity::getSubscription)
                .filter(Objects::nonNull)
                .mapToLong(sub -> resolvePlanMinor(sub.getPlan()))
                .sum();
        return PlatformAdminSummaryResponse.builder()
                .totalTenants(totalTenants)
                .activeTenants(activeTenants)
                .suspendedTenants(suspendedTenants)
                .pendingApprovals(pendingApprovals)
                .activeMembers(activeMembers)
                .monthlyRevenue(monthlyRevenue / 100)
                .openSupportTickets(0)
                .unresolvedIncidents(0)
                .latestHeartbeat(Instant.now())
                .build();
    }

    public List<PlatformTenantRowResponse> listTenants(String search, TenantStatus status, SubscriptionPlan plan, Integer limit) {
        List<TenantEntity> tenants = tenantRepository.findAllWithSubscription();
        List<PlatformTenantRowResponse> rows = new ArrayList<>();
        for (TenantEntity tenant : tenants) {
            if (status != null && tenant.getStatus() != status) {
                continue;
            }
            SubscriptionPlan tenantPlan = tenant.getSubscription() != null ? tenant.getSubscription().getPlan() : SubscriptionPlan.FREE;
            if (plan != null && tenantPlan != plan) {
                continue;
            }
            if (search != null && !search.isBlank()) {
                String lowered = search.toLowerCase(Locale.ROOT);
                boolean matches = tenant.getDisplayName().toLowerCase(Locale.ROOT).contains(lowered)
                        || tenant.getOwnerName().toLowerCase(Locale.ROOT).contains(lowered)
                        || tenant.getOwnerEmail().toLowerCase(Locale.ROOT).contains(lowered);
                if (!matches) {
                    continue;
                }
            }
            rows.add(toTenantRow(tenant));
            if (limit != null && rows.size() >= limit) {
                break;
            }
        }
        return rows;
    }

    public List<PlatformPaymentRecordResponse> listPayments(String tenantFilter, String statusFilter, Integer limit) {
        int pageSize = Math.max(limit != null ? limit : 12, 50);
        List<TenantSubscriptionEventEntity> events = eventRepository.findByEventTypeInOrderByOccurredAtDesc(
                PAYMENT_EVENT_TYPES,
                PageRequest.of(0, pageSize)
        );
        return events.stream()
                .filter(evt -> filterByTenant(evt, tenantFilter))
                .filter(evt -> filterByStatus(evt, statusFilter))
                .limit(limit != null ? limit : events.size())
                .map(this::toPaymentRecord)
                .collect(Collectors.toList());
    }

    public List<PlatformPriestApplicationResponse> listPriests() {
        return priestRepository.findAll().stream()
                .map(this::toPriestApplication)
                .collect(Collectors.toList());
    }

    public List<PlatformSupportTicketResponse> listSupportTickets() {
        return Collections.emptyList();
    }

    private boolean filterByTenant(TenantSubscriptionEventEntity event, String tenantFilter) {
        if (tenantFilter == null || tenantFilter.isBlank()) {
            return true;
        }
        return event.getTenant().getDisplayName().toLowerCase(Locale.ROOT).contains(tenantFilter.toLowerCase(Locale.ROOT));
    }

    private boolean filterByStatus(TenantSubscriptionEventEntity event, String statusFilter) {
        if (statusFilter == null) {
            return true;
        }
        String normalized = statusFilter.trim().toUpperCase(Locale.ROOT);
        if (normalized.equals("SUCCEEDED")) {
            return event.getEventType() == TenantSubscriptionEventType.PAYMENT_SUCCEEDED;
        }
        if (normalized.equals("FAILED")) {
            return event.getEventType() == TenantSubscriptionEventType.PAYMENT_FAILED;
        }
        if (normalized.equals("REFUNDED")) {
            return event.getEventType() == TenantSubscriptionEventType.PAYMENT_REFUNDED;
        }
        return true;
    }

    private PlatformPaymentRecordResponse toPaymentRecord(TenantSubscriptionEventEntity event) {
        SubscriptionPlan plan = event.getTenantSubscription().getPlan();
        long amountMinor = resolvePlanMinor(plan);
        String currency = billingCatalog.getCurrency();
        String status;
        if (event.getEventType() == TenantSubscriptionEventType.PAYMENT_SUCCEEDED) {
            status = "SUCCEEDED";
        } else if (event.getEventType() == TenantSubscriptionEventType.PAYMENT_FAILED) {
            status = "FAILED";
        } else if (event.getEventType() == TenantSubscriptionEventType.PAYMENT_REFUNDED) {
            status = "REFUNDED";
        } else {
            status = event.getEventType().name();
        }
        return PlatformPaymentRecordResponse.builder()
                .paymentId(event.getId())
                .tenantName(event.getTenant().getDisplayName())
                .amount(amountMinor / 100)
                .currency(currency)
                .status(status)
                .method(event.getTenantSubscription().getProvider().name())
                .capturedAt(event.getOccurredAt())
                .invoiceNumber(null)
                .note(null)
                .build();
    }

    private PlatformTenantRowResponse toTenantRow(TenantEntity tenant) {
        UUID tenantId = tenant.getId();
        SubscriptionPlan plan = tenant.getSubscription() != null ? tenant.getSubscription().getPlan() : SubscriptionPlan.FREE;
        SubscriptionStatus billingStatus = tenant.getSubscription() != null ? tenant.getSubscription().getStatus() : null;
        long activeMembers = memberRepository.countByTenantIdAndStatusValueIn(tenantId, ACTIVE_MEMBER_STATUSES)
                + childRepository.countByTenantIdAndStatusValueIn(tenantId, ACTIVE_MEMBER_STATUSES);
        long priests = priestRepository.countByChurch_Tenant_Id(tenantId);
        Instant renewal = tenant.getSubscription() != null ? tenant.getSubscription().getCurrentPeriodEndAt() : null;

        String region = tenant.getDefaultTimezone();
        String notes = billingStatus == SubscriptionStatus.SUSPENDED ? "Billing suspended" : null;

        return PlatformTenantRowResponse.builder()
                .tenantId(tenantId)
                .name(tenant.getDisplayName())
                .plan(plan)
                .status(tenant.getStatus())
                .billingStatus(billingStatus)
                .activeMembers(activeMembers)
                .priests(priests)
                .renewalDate(renewal)
                .contactEmail(tenant.getOwnerEmail())
                .accountOwner(tenant.getOwnerName())
                .region(region)
                .createdAt(tenant.getCreatedAt())
                .notes(notes)
                .build();
    }

    private PlatformPriestApplicationResponse toPriestApplication(PriestEntity priest) {
        String id = priest.getId() != null ? priest.getId().toString() : null;
        String fullName = String.join(" ", priest.getFirstName(), priest.getFatherName(), priest.getGrandFatherName());
        List<String> languages = priest.getLanguages().stream().toList();
        String location = priest.getChurch() != null ? priest.getChurch().getChurchName() : priest.getAddress() != null ? priest.getAddress().getCity() : "Unknown";
        Instant submittedAt = priest.getUser() != null ? priest.getUser().getCreatedAt() : null;
        UUID tenantId = priest.getTenant() != null ? priest.getTenant().getId() : null;
        int experienceYears = parseExperience(priest.getBirthdate());
        String notes = priest.getChurch() != null ? "Attached to " + priest.getChurch().getChurchName() : "Independent";
        return PlatformPriestApplicationResponse.builder()
                .priestId(id)
                .fullName(fullName)
                .languages(languages)
                .status(priest.getStatus())
                .location(location)
                .submittedAt(submittedAt)
                .assignedTenant(tenantId)
                .experienceYears(experienceYears)
                .notes(notes)
                .build();
    }

    private int parseExperience(String birthdate) {
        if (birthdate == null || birthdate.isBlank()) {
            return 0;
        }
        try {
            LocalDate parsed = LocalDate.parse(birthdate);
            return Period.between(parsed, LocalDate.now()).getYears();
        } catch (Exception ignored) {
            return 0;
        }
    }

    private long resolvePlanMinor(SubscriptionPlan plan) {
        try {
            return billingCatalog.resolve(plan).getAmountMinor();
        } catch (RuntimeException ex) {
            return 0;
        }
    }
}
