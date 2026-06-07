package com.anastasia.Anastasia_BackEnd.UnitTests.service.onboarding;

import com.anastasia.Anastasia_BackEnd.UnitTests.support.LenientMockitoTest;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.BillingInterval;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.BillingProvider;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.SubscriptionPlan;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.SubscriptionStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantSubscriptionEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantSubscriptionProviderLinkEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantType;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.WebhookEventReceiptEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantSubscriptionProviderLinkRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.WebhookEventReceiptRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.SubscriptionService;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.onboarding.TenantSelfServiceUpgradeBillingService;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.onboarding.TenantSubscriptionStripeWebhookService;
import com.stripe.model.Subscription;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@LenientMockitoTest
class TenantSubscriptionStripeWebhookServiceTest {

    @Mock
    private TenantSubscriptionProviderLinkRepository tenantSubscriptionProviderLinkRepository;

    @Mock
    private WebhookEventReceiptRepository webhookEventReceiptRepository;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private TenantSelfServiceUpgradeBillingService tenantSelfServiceUpgradeBillingService;

    @InjectMocks
    private TenantSubscriptionStripeWebhookService service;

    @Test
    void handleSubscriptionUpdated_persistsSanitizedWebhookReceipt() {
        UUID tenantId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        String payload = "{\"id\":\"evt_456\",\"email\":\"billing@example.com\",\"secret\":\"pii-value\"}";
        String signatureHeader = "t=456,v1=another-secret";
        Instant occurredAt = Instant.now();

        TenantEntity tenant = TenantEntity.builder()
                .id(tenantId)
                .displayName("Tenant")
                .slug("tenant-slug")
                .tenantType(TenantType.CHURCH)
                .status(TenantStatus.ACTIVE)
                .ownerName("Owner")
                .ownerEmail("owner@example.com")
                .phoneNumber("+15551230000")
                .build();

        TenantSubscriptionEntity subscription = TenantSubscriptionEntity.builder()
                .id(subscriptionId)
                .tenant(tenant)
                .plan(SubscriptionPlan.BASIC)
                .status(SubscriptionStatus.TRIALING)
                .billingInterval(BillingInterval.MONTHLY)
                .provider(BillingProvider.STRIPE)
                .build();

        TenantSubscriptionProviderLinkEntity providerLink = TenantSubscriptionProviderLinkEntity.builder()
                .tenantSubscription(subscription)
                .provider(BillingProvider.STRIPE)
                .providerSubscriptionId("sub_existing")
                .build();

        Subscription stripeSubscription = new Subscription();
        stripeSubscription.setId("sub_existing");
        stripeSubscription.setCustomer("cus_456");
        stripeSubscription.setStatus("active");

        when(tenantSubscriptionProviderLinkRepository
                .findByProviderAndProviderSubscriptionId(BillingProvider.STRIPE, "sub_existing"))
                .thenReturn(Optional.of(providerLink));
        when(webhookEventReceiptRepository.findByProviderAndEventId("STRIPE", "evt_456"))
                .thenReturn(Optional.empty());
        when(webhookEventReceiptRepository.save(any(WebhookEventReceiptEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        boolean handled = service.handleSubscriptionUpdated(
                "evt_456",
                occurredAt,
                payload,
                signatureHeader,
                stripeSubscription
        );

        assertThat(handled).isTrue();

        ArgumentCaptor<WebhookEventReceiptEntity> receiptCaptor =
                ArgumentCaptor.forClass(WebhookEventReceiptEntity.class);
        verify(webhookEventReceiptRepository, times(2)).save(receiptCaptor.capture());

        List<WebhookEventReceiptEntity> savedReceipts = receiptCaptor.getAllValues();
        assertThat(savedReceipts).hasSize(2);

        for (WebhookEventReceiptEntity receipt : savedReceipts) {
            assertThat(receipt.getTenantId()).isEqualTo(tenantId);
            assertThat(receipt.getTenantSubscriptionId()).isEqualTo(subscriptionId);
            assertThat(receipt.getPayload()).contains("\"payloadSize\":").contains("\"payloadSha256\":\"");
            assertThat(receipt.getPayload()).doesNotContain("billing@example.com");
            assertThat(receipt.getPayload()).doesNotContain("pii-value");
            assertThat(receipt.getPayload()).doesNotContain(payload);
            assertThat(receipt.getSignatureHeader()).isNull();
        }

        assertThat(savedReceipts.get(savedReceipts.size() - 1).getProcessingResult()).isNotNull();
    }
}
