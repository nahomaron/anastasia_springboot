package com.anastasia.Anastasia_BackEnd.modules.payments.application.query;

import com.anastasia.Anastasia_BackEnd.modules.accounting.model.Fund;
import com.anastasia.Anastasia_BackEnd.modules.accounting.repository.FundRepository;
import com.anastasia.Anastasia_BackEnd.modules.payments.domain.model.PaymentIntent;
import com.anastasia.Anastasia_BackEnd.modules.payments.domain.model.PaymentPurpose;
import com.anastasia.Anastasia_BackEnd.modules.payments.domain.model.PaymentStatus;
import com.anastasia.Anastasia_BackEnd.modules.payments.repository.PaymentIntentRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.MemberLifecycleStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.Adult_MemberEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import com.anastasia.Anastasia_BackEnd.UnitTests.support.LenientMockitoTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@LenientMockitoTest
@Tag("experimental")
class PaymentQueryServiceTest {

    @Mock private PaymentIntentRepository paymentRepo;
    @Mock private MemberRepository memberRepo;
    @Mock private FundRepository fundRepo;
    @InjectMocks private PaymentQueryService service;

    private final UUID tenantId = UUID.randomUUID();
    private final Pageable pageable = PageRequest.of(0, 10);

    private PaymentIntent intent;

    @BeforeEach
    void initIntent() {
        intent = PaymentIntent.newInitiated(
                tenantId,
                PaymentPurpose.TITHE,
                5_000L,
                "USD",
                17L,
                UUID.randomUUID(),
                "donor@example.org",
                "99",
                "idem-key");
        intent.setStatus(PaymentStatus.CAPTURED);
    }

    @Test
    void findAll_enrichesMemberAndFundDetails() {
        intent.setFundId("99");
        intent.setMemberId(17L);

        var member = new Adult_MemberEntity();
        member.setId(17L);
        member.setTenantId(tenantId);
        member.setFirstName("Selam");
        member.setFatherName("Gebremedhin");
        member.setGrandFatherName("Haile");
        member.setEmail("selam@faith.org");
        member.setUserId(UUID.randomUUID());
        member.setStatusValue(MemberLifecycleStatus.ACTIVE);

        var fund = Fund.builder()
                .tenantId(tenantId)
                .name("Building Fund")
                .build();

        var page = new PageImpl<>(List.of(intent), pageable, 1);
        when(paymentRepo.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable)).thenReturn(page);
        when(memberRepo.findByIdAndTenantId(17L, tenantId)).thenReturn(Optional.of(member));
        when(fundRepo.findByIdAndTenantId(99L, tenantId)).thenReturn(Optional.of(fund));

        var result = service.findAll(tenantId, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        var view = result.getContent().get(0);
        assertThat(view.getMemberName()).contains("Selam");
        assertThat(view.getMemberEmail()).isEqualTo("selam@faith.org");
        assertThat(view.getFundName()).isEqualTo("Building Fund");
        verify(paymentRepo).findByTenantIdOrderByCreatedAtDesc(tenantId, pageable);
    }

    @Test
    void findById_returnsViewWhenIntentExists() {
        when(paymentRepo.findByIdAndTenantId(intent.getId(), tenantId)).thenReturn(Optional.of(intent));

        var view = service.findById(tenantId, intent.getId());

        assertThat(view).isNotNull();
        assertThat(view.getStatus()).isEqualTo(intent.getStatus().name());
        assertThat(view.getAmount()).isEqualTo(intent.getAmount().getAmount());
    }

    @Test
    void findById_throwsWhenMissing() {
        when(paymentRepo.findByIdAndTenantId(intent.getId(), tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(tenantId, intent.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Payment not found");
    }
}
