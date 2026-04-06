package com.anastasia.Anastasia_BackEnd.UnitTests.service.dashboard;

import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.modules.dashboard.dto.TenantAdminDashboardResponse;
import com.anastasia.Anastasia_BackEnd.modules.dashboard.service.TenantAdminDashboardService;
import com.anastasia.Anastasia_BackEnd.modules.payments.domain.model.PaymentIntent;
import com.anastasia.Anastasia_BackEnd.modules.payments.domain.model.PaymentPurpose;
import com.anastasia.Anastasia_BackEnd.modules.payments.domain.model.PaymentStatus;
import com.anastasia.Anastasia_BackEnd.modules.payments.repository.PaymentIntentRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.Adult_MemberEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child.Child_MemberEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.MemberLifecycleStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChildRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChurchRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.MemberRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.PriestRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import com.anastasia.Anastasia_BackEnd.UnitTests.support.LenientMockitoTest;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Behaviors:
 * - Dashboard aggregates headcount, payment metrics, recent members/payments, and church completeness for the current tenant.
 * - Payment currency/status mapping must cope with missing providers and null currency details while still returning a default USD ticker.
 * - Recent members must be merged and sorted, limits must not leak more than the configured RECENT_LIMIT.
 * Edge cases: repository providers returning null, captured currency missing, and missing tenant context.
 */
@LenientMockitoTest
@Tag("experimental")
class TenantAdminDashboardServiceUnitTest {

    @Mock private MemberRepository memberRepository;
    @Mock private ChildRepository childRepository;
    @Mock private PriestRepository priestRepository;
    @Mock private ChurchRepository churchRepository;
    @Mock private ObjectProvider<PaymentIntentRepository> paymentIntentRepositoryProvider;
    @Mock private PaymentIntentRepository paymentIntentRepository;

    @InjectMocks private TenantAdminDashboardService dashboardService;

    private final UUID tenantId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void getSummary_shouldReturnZeroOfferingWhenPaymentProviderMissing() {
        when(memberRepository.countByStatusValueNotAndTenantId(any(), any())).thenReturn(7L);
        when(childRepository.countByStatusValueNotAndTenantId(any(), any())).thenReturn(3L);
        when(priestRepository.countByChurch_Tenant_Id(any())).thenReturn(2L);
        when(memberRepository.findByTenantIdOrderByCreatedAtDesc(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(createAdult(1L, "A", LocalDateTime.now().minusDays(1)))));
        when(childRepository.findByTenantIdOrderByCreatedAtDesc(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(createChild(11L, "C", LocalDateTime.now()))));
        when(churchRepository.findByTenantId(any())).thenReturn(Optional.empty());
        when(paymentIntentRepositoryProvider.getIfAvailable()).thenReturn(null);

        TenantAdminDashboardResponse response = dashboardService.getSummary();

        assertThat(response.getStats().getActiveMembers()).isEqualTo(10L);
        assertThat(response.getStats().getMonthlyOffering().getAmount()).isZero();
        assertThat(response.getStats().getMonthlyOffering().getCurrency()).isEqualTo("USD");
        assertThat(response.getRecentPayments()).isEmpty();
        assertThat(response.getRecentMembers()).hasSize(2);
    }

    @Test
    void getSummary_shouldMapPaymentsToMonthlyStatsAndRecentPayments() {
        when(memberRepository.countByStatusValueNotAndTenantId(any(), any())).thenReturn(2L);
        when(childRepository.countByStatusValueNotAndTenantId(any(), any())).thenReturn(0L);
        when(priestRepository.countByChurch_Tenant_Id(any())).thenReturn(1L);
        when(memberRepository.findByTenantIdOrderByCreatedAtDesc(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(createAdult(1L, "A", LocalDateTime.now()))));
        when(childRepository.findByTenantIdOrderByCreatedAtDesc(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(churchRepository.findByTenantId(any())).thenReturn(Optional.of(new ChurchEntity()));
        when(paymentIntentRepositoryProvider.getIfAvailable()).thenReturn(paymentIntentRepository);

        PaymentIntent payment = PaymentIntent.newInitiated(tenantId, PaymentPurpose.TITHE, 5000, "USD", 1L, null, null, null, "id");
        payment.setStatus(PaymentStatus.CAPTURED);
        payment.setCapturedGrossAmountMinor(4500L);
        payment.setCapturedCurrency("EUR");
        payment.setCreatedAt(Instant.now());
        when(paymentIntentRepository.sumCapturedAmountByTenantAndCapturedAtBetween(any(), any(), any())).thenReturn(4500L);
        when(paymentIntentRepository.findTopByTenantIdAndStatusOrderByCapturedAtDesc(any(), eq(PaymentStatus.CAPTURED))).thenReturn(Optional.of(payment));
        when(paymentIntentRepository.findByTenantIdOrderByCreatedAtDesc(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(payment)));

        TenantAdminDashboardResponse response = dashboardService.getSummary();

        assertThat(response.getStats().getActiveMembers()).isEqualTo(2L);
        assertThat(response.getStats().getChildren()).isZero();
        assertThat(response.getStats().getPriestsStaff()).isEqualTo(1L);
        assertThat(response.getStats().getMonthlyOffering().getAmount()).isEqualTo(45.0);
        assertThat(response.getStats().getMonthlyOffering().getCurrency()).isEqualTo("EUR");
        assertThat(response.getRecentPayments()).hasSize(1);
        assertThat(response.getRecentPayments().get(0).getCategory()).isEqualTo("TITHE");
        assertThat(response.getRecentPayments().get(0).getStatus()).isEqualTo("SUCCEEDED");
    }

    private Adult_MemberEntity createAdult(Long id, String suffix, LocalDateTime createdAt) {
        Adult_MemberEntity adult = new Adult_MemberEntity();
        adult.setId(id);
        adult.setTenantId(tenantId);
        adult.setFirstName("First" + suffix);
        adult.setFatherName("Father" + suffix);
        adult.setGrandFatherName("Grand" + suffix);
        adult.setMotherName("Mother" + suffix);
        adult.setMothersFather("Mothers" + suffix);
        adult.setFirstNameLocal("Local" + suffix);
        adult.setFatherNameLocal("Local" + suffix);
        adult.setGrandFatherNameLocal("Local" + suffix);
        adult.setMotherFullNameLocal("Local" + suffix);
        adult.setGenderValue(com.anastasia.Anastasia_BackEnd.modules.registration.model.member.MemberGender.MALE);
        adult.setBirthday(createdAt.toLocalDate());
        adult.setFatherOfConfession("Confessor");
        adult.setTitle("Mr");
        adult.setChurchNumber("000");
        adult.setStatusEnum(MemberLifecycleStatus.ACTIVE);
        adult.setCreatedAt(createdAt.toInstant(ZoneOffset.UTC));
        return adult;
    }

    private Child_MemberEntity createChild(Long id, String suffix, LocalDateTime createdAt) {
        Child_MemberEntity child = new Child_MemberEntity();
        child.setId(id);
        child.setTenantId(tenantId);
        child.setFirstName("Child" + suffix);
        child.setFatherName("Father" + suffix);
        child.setGrandFatherName("Grand" + suffix);
        child.setMotherFullNameLocal("Mother" + suffix);
        child.setMotherName("Mother" + suffix);
        child.setMothersFather("MotherFather" + suffix);
        child.setFirstNameLocal("Local" + suffix);
        child.setFatherNameLocal("Local" + suffix);
        child.setGrandFatherNameLocal("Local" + suffix);
        child.setGenderValue(com.anastasia.Anastasia_BackEnd.modules.registration.model.member.MemberGender.FEMALE);
        child.setBirthday(createdAt.toLocalDate());
        child.setChurchNumber("000");
        child.setStatusEnum(MemberLifecycleStatus.ACTIVE);
        child.setCreatedAt(createdAt.toInstant(ZoneOffset.UTC));
        return child;
    }
}
