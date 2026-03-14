package com.anastasia.Anastasia_BackEnd.modules.dashboard.service;

import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.modules.dashboard.dto.MemberOverviewItem;
import com.anastasia.Anastasia_BackEnd.modules.dashboard.dto.MonthlyOffering;
import com.anastasia.Anastasia_BackEnd.modules.dashboard.dto.TenantAdminDashboardResponse;
import com.anastasia.Anastasia_BackEnd.modules.dashboard.dto.TenantAdminStats;
import com.anastasia.Anastasia_BackEnd.modules.dashboard.dto.TenantPaymentItem;
import com.anastasia.Anastasia_BackEnd.modules.payments.domain.model.PaymentIntent;
import com.anastasia.Anastasia_BackEnd.modules.payments.domain.model.PaymentPurpose;
import com.anastasia.Anastasia_BackEnd.modules.payments.domain.model.PaymentStatus;
import com.anastasia.Anastasia_BackEnd.modules.payments.repository.PaymentIntentRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.Adult_MemberEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child.Child_MemberEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.MemberStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChurchRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChildRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.MemberRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.PriestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class TenantAdminDashboardService {

    private static final int RECENT_LIMIT = 5;

    private final MemberRepository memberRepository;
    private final ChildRepository childRepository;
    private final PriestRepository priestRepository;
    private final ChurchRepository churchRepository;
    private final ObjectProvider<PaymentIntentRepository> paymentIntentRepositoryProvider;

    public TenantAdminDashboardResponse getSummary() {
        UUID tenantId = requireTenantId();

        long adultCount = memberRepository.countByStatusNotAndTenantId(MemberStatus.PENDING.name(), tenantId);
        long childCount = childRepository.countByStatusNotAndTenantId(MemberStatus.PENDING.name(), tenantId);
        long priestsCount = priestRepository.countByChurch_Tenant_Id(tenantId);

        MonthlyOffering offering = buildMonthlyOffering(tenantId);
        List<MemberOverviewItem> recentMembers = buildRecentMembers(tenantId);
        List<TenantPaymentItem> recentPayments = buildRecentPayments(tenantId);
        boolean churchProfileComplete = churchRepository.findByTenantId(tenantId)
                .map(church -> church.isComplete())
                .orElse(false);

        TenantAdminStats stats = TenantAdminStats.builder()
                .activeMembers(adultCount + childCount)
                .children(childCount)
                .priestsStaff(priestsCount)
                .monthlyOffering(offering)
                .build();

        return TenantAdminDashboardResponse.builder()
                .stats(stats)
                .recentMembers(recentMembers)
                .recentPayments(recentPayments)
                .churchProfileComplete(churchProfileComplete)
                .build();
    }

    private MonthlyOffering buildMonthlyOffering(UUID tenantId) {
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

        long totalMinor = paymentIntentRepository.sumCapturedAmountByTenantAndCapturedAtBetween(tenantId, start, end);
        String currency = paymentIntentRepository.findTopByTenantIdAndStatusOrderByCapturedAtDesc(tenantId, PaymentStatus.CAPTURED)
                .map(this::resolveCurrency)
                .orElse("USD");

        return MonthlyOffering.builder()
                .amount(totalMinor / 100.0)
                .currency(currency)
                .build();
    }

    private List<MemberOverviewItem> buildRecentMembers(UUID tenantId) {
        var page = PageRequest.of(0, RECENT_LIMIT);
        List<MemberOverviewItem> adults = memberRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, page)
                .map(this::toMemberOverview)
                .getContent();
        List<MemberOverviewItem> children = childRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, page)
                .map(this::toMemberOverview)
                .getContent();

        return Stream.concat(adults.stream(), children.stream())
                .sorted(Comparator.comparing(MemberOverviewItem::getRegisteredAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(RECENT_LIMIT)
                .toList();
    }

    private List<TenantPaymentItem> buildRecentPayments(UUID tenantId) {
        PaymentIntentRepository paymentIntentRepository = paymentIntentRepositoryProvider.getIfAvailable();
        if (paymentIntentRepository == null) {
            return List.of();
        }
        var page = PageRequest.of(0, RECENT_LIMIT);
        return paymentIntentRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, page)
                .map(this::toPaymentItem)
                .getContent();
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

    private TenantPaymentItem toPaymentItem(PaymentIntent payment) {
        long amountMinor = resolveAmountMinor(payment);
        return TenantPaymentItem.builder()
                .category(mapCategory(payment.getPurpose()))
                .amount(amountMinor / 100.0)
                .currency(resolveCurrency(payment))
                .status(mapStatus(payment.getStatus()))
                .date(payment.getCreatedAt())
                .build();
    }

    private long resolveAmountMinor(PaymentIntent payment) {
        if (payment.getCapturedGrossAmountMinor() != null) {
            return payment.getCapturedGrossAmountMinor();
        }
        if (payment.getAmount() != null) {
            return payment.getAmount().getAmount();
        }
        return 0;
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

    private String mapCategory(PaymentPurpose purpose) {
        if (purpose == null) {
            return "DONATION";
        }
        return switch (purpose) {
            case TITHE -> "TITHE";
            case SPECIAL_EVENT_PAYMENT -> "EVENT";
            case DONATION, CONTRIBUTION, SUNDAY_SCHOOL_DONATION -> "DONATION";
            default -> "DONATION";
        };
    }

    private String mapStatus(PaymentStatus status) {
        if (status == null) {
            return "PENDING";
        }
        return switch (status) {
            case CAPTURED -> "SUCCEEDED";
            case FAILED -> "FAILED";
            default -> "PENDING";
        };
    }

    private String fullName(String first, String father, String grandFather) {
        return Stream.of(first, father, grandFather)
                .filter(value -> value != null && !value.isBlank())
                .reduce((a, b) -> a + " " + b)
                .orElse("");
    }

    private UUID requireTenantId() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant context is missing for dashboard data");
        }
        return tenantId;
    }
}
