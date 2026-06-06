package com.anastasia.Anastasia_BackEnd.UnitTests.registration.onboarding;

import com.anastasia.Anastasia_BackEnd.UnitTests.support.LenientMockitoTest;
import com.anastasia.Anastasia_BackEnd.common.i18n.LocalizedMessageService;
import com.anastasia.Anastasia_BackEnd.modules.payments.stripe.StripeClient;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.entitlement.SubscriptionUpgradeCheckoutResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.BillingProvider;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.SubscriptionPlan;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.SubscriptionStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantSubscriptionEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantSubscriptionUpgradeRequestEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantSubscriptionProviderLinkRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantSubscriptionUpgradeRequestRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.SubscriptionService;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.onboarding.TenantPlanBillingCatalog;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.onboarding.TenantSelfServiceUpgradeBillingService;
import com.stripe.model.checkout.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@LenientMockitoTest
class TenantSelfServiceUpgradeBillingServiceTest {

    @Mock private SubscriptionService subscriptionService;
    @Mock private TenantSubscriptionProviderLinkRepository tenantSubscriptionProviderLinkRepository;
    @Mock private TenantSubscriptionUpgradeRequestRepository tenantSubscriptionUpgradeRequestRepository;
    @Mock private StripeClient stripeClient;
    @Mock private LocalizedMessageService messageService;

    private TenantPlanBillingCatalog billingCatalog;

    @InjectMocks
    private TenantSelfServiceUpgradeBillingService upgradeBillingService;

    @BeforeEach
    void setUp() {
        billingCatalog = new TenantPlanBillingCatalog(messageService);
        TenantPlanBillingCatalog.PlanPrice basic = new TenantPlanBillingCatalog.PlanPrice();
        basic.setPriceId("price_basic");
        basic.setAmountMinor(1900L);
        billingCatalog.getPlans().put(SubscriptionPlan.BASIC, basic);
        upgradeBillingService = new TenantSelfServiceUpgradeBillingService(
                subscriptionService,
                billingCatalog,
                tenantSubscriptionProviderLinkRepository,
                tenantSubscriptionUpgradeRequestRepository,
                stripeClient,
                messageService
        );
        when(messageService.get(anyString(), anyString(), any())).thenAnswer(invocation -> invocation.getArgument(1));
        when(messageService.get(anyString(), anyString())).thenAnswer(invocation -> invocation.getArgument(1));
    }

    @Test
    void createUpgradeCheckoutCreatesStripeCheckoutForFreeToBasic() throws Exception {
        TenantSubscriptionEntity subscription = subscription(SubscriptionPlan.FREE);
        when(tenantSubscriptionUpgradeRequestRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.empty());
        when(tenantSubscriptionUpgradeRequestRepository.findFirstByTenantSubscription_IdAndStatusInOrderByCreatedAtDesc(any(), any()))
                .thenReturn(Optional.empty());
        when(subscriptionService.syncSubscriptionState(subscription.getTenant().getId(), subscription.getUpdatedByUserId()))
                .thenReturn(subscription);
        when(tenantSubscriptionProviderLinkRepository.findByTenantSubscription_IdAndProvider(subscription.getId(), BillingProvider.STRIPE))
                .thenReturn(Optional.empty());
        when(tenantSubscriptionUpgradeRequestRepository.save(any(TenantSubscriptionUpgradeRequestEntity.class)))
                .thenAnswer(invocation -> {
                    TenantSubscriptionUpgradeRequestEntity saved = invocation.getArgument(0);
                    if (saved.getId() == null) {
                        saved.setId(UUID.randomUUID());
                    }
                    return saved;
                });
        Session stripeSession = org.mockito.Mockito.mock(Session.class);
        when(stripeSession.getId()).thenReturn("cs_upgrade_123");
        when(stripeSession.getUrl()).thenReturn("https://checkout.stripe.test/cs_upgrade_123");
        when(stripeClient.createTenantUpgradeSubscriptionCheckoutSession(anyString(), any(), anyString(), anyString(), anyString()))
                .thenReturn(stripeSession);

        SubscriptionUpgradeCheckoutResponse response = upgradeBillingService.createUpgradeCheckout(
                subscription.getTenant().getId(),
                SubscriptionPlan.BASIC,
                "idem-1",
                subscription.getUpdatedByUserId()
        );

        assertThat(response.getCurrentPlan()).isEqualTo(SubscriptionPlan.FREE);
        assertThat(response.getTargetPlan()).isEqualTo(SubscriptionPlan.BASIC);
        assertThat(response.getCheckoutSessionId()).isEqualTo("cs_upgrade_123");
        assertThat(response.getCheckoutUrl()).contains("checkout.stripe.test");
    }

    @Test
    void createUpgradeCheckoutRejectsUnsupportedSelfServiceUpgrade() {
        TenantSubscriptionEntity subscription = subscription(SubscriptionPlan.BASIC);
        when(tenantSubscriptionUpgradeRequestRepository.findByIdempotencyKey("idem-2")).thenReturn(Optional.empty());
        when(subscriptionService.syncSubscriptionState(subscription.getTenant().getId(), subscription.getUpdatedByUserId()))
                .thenReturn(subscription);

        assertThrows(IllegalArgumentException.class, () -> upgradeBillingService.createUpgradeCheckout(
                subscription.getTenant().getId(),
                SubscriptionPlan.ADVANCED,
                "idem-2",
                subscription.getUpdatedByUserId()
        ));
    }

    private TenantSubscriptionEntity subscription(SubscriptionPlan plan) {
        TenantEntity tenant = TenantEntity.builder()
                .id(UUID.randomUUID())
                .build();
        return TenantSubscriptionEntity.builder()
                .id(UUID.randomUUID())
                .tenant(tenant)
                .plan(plan)
                .status(SubscriptionStatus.TRIALING)
                .provider(BillingProvider.MANUAL)
                .updatedByUserId(UUID.randomUUID())
                .build();
    }
}
