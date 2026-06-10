package com.anastasia.Anastasia_BackEnd.UnitTests.service.registration;

import com.anastasia.Anastasia_BackEnd.UnitTests.support.LenientMockitoTest;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.BillingProvider;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.PlanChangeTiming;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.SubscriptionPlan;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.SubscriptionStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantSubscriptionEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.SubscriptionPlanHistoryRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantSubscriptionEventRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantSubscriptionProviderLinkRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantSubscriptionRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.SubscriptionServiceImpl;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.TenantWorkspaceLifecycleService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@LenientMockitoTest
class SubscriptionServiceImplUnitTest {

    @Mock private TenantSubscriptionRepository tenantSubscriptionRepository;
    @Mock private TenantSubscriptionProviderLinkRepository tenantSubscriptionProviderLinkRepository;
    @Mock private TenantSubscriptionEventRepository tenantSubscriptionEventRepository;
    @Mock private SubscriptionPlanHistoryRepository subscriptionPlanHistoryRepository;
    @Mock private TenantRepository tenantRepository;
    @Mock private TenantWorkspaceLifecycleService tenantWorkspaceLifecycleService;

    @InjectMocks
    private SubscriptionServiceImpl subscriptionService;

    @Test
    void requestPlanChangeRejectsUpgradeWithoutCheckout() {
        TenantSubscriptionEntity subscription = subscription(SubscriptionPlan.FREE, SubscriptionStatus.TRIALING);
        when(tenantSubscriptionRepository.findByTenantId(subscription.getTenant().getId()))
                .thenReturn(Optional.of(subscription));

        assertThrows(IllegalArgumentException.class, () -> subscriptionService.requestPlanChange(
                subscription.getTenant().getId(),
                SubscriptionPlan.BASIC,
                PlanChangeTiming.IMMEDIATE,
                "upgrade",
                UUID.randomUUID()
        ));

        verify(tenantSubscriptionRepository, never()).save(any(TenantSubscriptionEntity.class));
    }

    @Test
    void activatePaidPlanUpdatesPlanAndProviderState() {
        TenantSubscriptionEntity subscription = subscription(SubscriptionPlan.FREE, SubscriptionStatus.TRIALING);
        when(tenantSubscriptionRepository.findByTenantId(subscription.getTenant().getId()))
                .thenReturn(Optional.of(subscription));
        when(tenantSubscriptionProviderLinkRepository.findByTenantSubscription_IdAndProvider(subscription.getId(), BillingProvider.STRIPE))
                .thenReturn(Optional.empty());
        when(tenantSubscriptionRepository.save(any(TenantSubscriptionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Instant paymentAt = Instant.parse("2026-06-06T12:00:00Z");
        TenantSubscriptionEntity updated = subscriptionService.activatePaidPlan(
                subscription.getTenant().getId(),
                SubscriptionPlan.BASIC,
                paymentAt,
                BillingProvider.STRIPE,
                "cus_123",
                "sub_123",
                "price_basic",
                UUID.randomUUID(),
                "Self-service checkout upgrade confirmed",
                "evt_123"
        );

        assertThat(updated.getPlan()).isEqualTo(SubscriptionPlan.BASIC);
        assertThat(updated.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(updated.getProvider()).isEqualTo(BillingProvider.STRIPE);
        assertThat(updated.getLastPaymentAt()).isEqualTo(paymentAt);
        assertThat(updated.getProviderLinks()).hasSize(1);
        assertThat(updated.getProviderLinks().iterator().next().getProviderSubscriptionId()).isEqualTo("sub_123");
    }

    @Test
    void cancelSubscriptionImmediatelyEndsAccessAndTriggersLifecycleSync() {
        TenantSubscriptionEntity subscription = subscription(SubscriptionPlan.BASIC, SubscriptionStatus.ACTIVE);
        subscription.setCurrentPeriodEndAt(Instant.parse("2026-07-01T00:00:00Z"));
        when(tenantSubscriptionRepository.findByTenantId(subscription.getTenant().getId()))
                .thenReturn(Optional.of(subscription));
        when(tenantSubscriptionRepository.save(any(TenantSubscriptionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Instant before = Instant.now();
        TenantSubscriptionEntity updated = subscriptionService.cancelSubscription(
                subscription.getTenant().getId(),
                false,
                UUID.randomUUID()
        );
        Instant after = Instant.now();

        assertThat(updated.getStatus()).isEqualTo(SubscriptionStatus.CANCELED);
        assertThat(updated.getCanceledAt()).isBetween(before, after);
        assertThat(updated.getEndedAt()).isEqualTo(updated.getCanceledAt());
        assertThat(updated.getCurrentPeriodEndAt()).isEqualTo(updated.getCanceledAt());
        verify(tenantWorkspaceLifecycleService).syncTenantLifecycle(subscription.getTenant().getId(), updated.getUpdatedByUserId());
    }

    private TenantSubscriptionEntity subscription(SubscriptionPlan plan, SubscriptionStatus status) {
        TenantEntity tenant = TenantEntity.builder()
                .id(UUID.randomUUID())
                .build();
        return TenantSubscriptionEntity.builder()
                .id(UUID.randomUUID())
                .tenant(tenant)
                .plan(plan)
                .status(status)
                .provider(BillingProvider.MANUAL)
                .build();
    }
}
