package com.anastasia.Anastasia_BackEnd.UnitTests.service.registration;

import com.anastasia.Anastasia_BackEnd.UnitTests.support.LenientMockitoTest;
import com.anastasia.Anastasia_BackEnd.common.i18n.LocalizedMessageService;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.entitlement.TenantBillingOverrideRequest;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.BillingOverrideType;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.SubscriptionPlan;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantBillingOverrideEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantSubscriptionEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantBillingOverrideAuditRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantBillingOverrideRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.entitlement.TenantBillingOverrideService;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.onboarding.TenantPlanBillingCatalog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@LenientMockitoTest
class TenantBillingOverrideServiceTest {

    @Mock private TenantBillingOverrideRepository tenantBillingOverrideRepository;
    @Mock private TenantBillingOverrideAuditRepository tenantBillingOverrideAuditRepository;
    @Mock private TenantRepository tenantRepository;
    @Mock private TenantPlanBillingCatalog billingCatalog;
    @Mock private LocalizedMessageService messageService;

    @InjectMocks private TenantBillingOverrideService service;

    private UUID tenantId;
    private TenantEntity tenant;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        tenant = new TenantEntity();
        tenant.setId(tenantId);

        TenantPlanBillingCatalog.PlanPrice basic = new TenantPlanBillingCatalog.PlanPrice();
        basic.setPriceId("price_basic");
        basic.setAmountMinor(5000L);
        when(billingCatalog.resolve(SubscriptionPlan.BASIC)).thenReturn(basic);
        when(billingCatalog.getCurrency()).thenReturn("USD");
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(messageService.get(anyString(), anyString(), any())).thenAnswer(invocation -> invocation.getArgument(1));
        doAnswer(invocation -> invocation.getArgument(0)).when(tenantBillingOverrideRepository).save(any(TenantBillingOverrideEntity.class));
        doAnswer(invocation -> invocation.getArgument(0)).when(tenantBillingOverrideAuditRepository).save(any());
    }

    @Test
    void createOverride_rejectsInvalidPercentDiscount() {
        TenantBillingOverrideRequest request = TenantBillingOverrideRequest.builder()
                .overrideType(BillingOverrideType.PERCENT_DISCOUNT)
                .startsAt(Instant.now())
                .endsAt(Instant.now().plusSeconds(3600))
                .discountPercent(BigDecimal.valueOf(120))
                .build();

        assertThatThrownBy(() -> service.createOverride(tenantId, request, UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createOverride_rejectsOverlappingActiveOverride() {
        TenantBillingOverrideEntity existing = TenantBillingOverrideEntity.builder()
                .id(UUID.randomUUID())
                .tenant(tenant)
                .overrideType(BillingOverrideType.FREE_ACCESS)
                .startsAt(Instant.parse("2026-01-01T00:00:00Z"))
                .endsAt(Instant.parse("2026-02-01T00:00:00Z"))
                .active(true)
                .build();
        when(tenantBillingOverrideRepository.findActiveCandidatesByTenantId(tenantId)).thenReturn(List.of(existing));

        TenantBillingOverrideRequest request = TenantBillingOverrideRequest.builder()
                .overrideType(BillingOverrideType.PERCENT_DISCOUNT)
                .startsAt(Instant.parse("2026-01-15T00:00:00Z"))
                .endsAt(Instant.parse("2026-03-01T00:00:00Z"))
                .discountPercent(BigDecimal.valueOf(25))
                .build();

        assertThatThrownBy(() -> service.createOverride(tenantId, request, UUID.randomUUID()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void calculateCharge_appliesPercentDiscount() {
        TenantBillingOverrideEntity override = TenantBillingOverrideEntity.builder()
                .id(UUID.randomUUID())
                .tenant(tenant)
                .overrideType(BillingOverrideType.PERCENT_DISCOUNT)
                .startsAt(Instant.parse("2026-01-01T00:00:00Z"))
                .endsAt(Instant.parse("2026-02-01T00:00:00Z"))
                .discountPercent(BigDecimal.valueOf(25))
                .currency("usd")
                .active(true)
                .build();
        when(tenantBillingOverrideRepository.findActiveCandidatesByTenantId(tenantId)).thenReturn(List.of(override));

        var result = service.calculateCharge(tenantId, SubscriptionPlan.BASIC, Instant.parse("2026-01-10T00:00:00Z"));

        assertThat(result.getNormalAmountMinor()).isEqualTo(5000L);
        assertThat(result.getDiscountAmountMinor()).isEqualTo(1250L);
        assertThat(result.getEffectiveAmountMinor()).isEqualTo(3750L);
        assertThat(result.getAppliedBillingOverrideType()).isEqualTo(BillingOverrideType.PERCENT_DISCOUNT);
        assertThat(result.getCurrency()).isEqualTo("USD");
    }

    @Test
    void calculateCharge_ignoresExpiredOverride() {
        TenantBillingOverrideEntity expired = TenantBillingOverrideEntity.builder()
                .id(UUID.randomUUID())
                .tenant(tenant)
                .overrideType(BillingOverrideType.FREE_ACCESS)
                .startsAt(Instant.parse("2026-01-01T00:00:00Z"))
                .endsAt(Instant.parse("2026-01-05T00:00:00Z"))
                .active(true)
                .build();
        when(tenantBillingOverrideRepository.findActiveCandidatesByTenantId(tenantId)).thenReturn(List.of(expired));

        var result = service.calculateCharge(tenantId, SubscriptionPlan.BASIC, Instant.parse("2026-01-10T00:00:00Z"));

        assertThat(result.getEffectiveAmountMinor()).isEqualTo(5000L);
        assertThat(result.getAppliedBillingOverrideType()).isNull();
    }

    @Test
    void revokeOverride_marksInactiveAndWritesAudit() {
        UUID overrideId = UUID.randomUUID();
        TenantBillingOverrideEntity entity = TenantBillingOverrideEntity.builder()
                .id(overrideId)
                .tenant(tenant)
                .overrideType(BillingOverrideType.FIXED_PRICE)
                .startsAt(Instant.now().minusSeconds(600))
                .endsAt(Instant.now().plusSeconds(3600))
                .fixedAmountMinor(1000L)
                .active(true)
                .build();
        when(tenantBillingOverrideRepository.findByIdAndTenantId(overrideId, tenantId)).thenReturn(Optional.of(entity));

        service.revokeOverride(tenantId, overrideId, "Expired offer", UUID.randomUUID());

        ArgumentCaptor<TenantBillingOverrideEntity> captor = ArgumentCaptor.forClass(TenantBillingOverrideEntity.class);
        verify(tenantBillingOverrideRepository).save(captor.capture());
        assertThat(captor.getValue().isActive()).isFalse();
        assertThat(captor.getValue().getRevokedAt()).isNotNull();
        assertThat(captor.getValue().getReason()).isEqualTo("Expired offer");
        verify(tenantBillingOverrideAuditRepository).save(any());
    }

    @Test
    void preservesAccess_allowsTrialAndCompedStates() {
        TenantSubscriptionEntity subscription = new TenantSubscriptionEntity();
        subscription.setTenant(tenant);
        TenantBillingOverrideEntity override = TenantBillingOverrideEntity.builder()
                .id(UUID.randomUUID())
                .tenant(tenant)
                .overrideType(BillingOverrideType.TRIAL_EXTENSION)
                .startsAt(Instant.now().minusSeconds(60))
                .endsAt(Instant.now().plusSeconds(600))
                .active(true)
                .build();
        when(tenantBillingOverrideRepository.findActiveCandidatesByTenantId(tenantId)).thenReturn(List.of(override));

        assertThat(service.preservesAccess(subscription, Instant.now())).isTrue();
    }
}
