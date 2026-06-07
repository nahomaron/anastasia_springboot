package com.anastasia.Anastasia_BackEnd.UnitTests.service.onboarding;

import com.anastasia.Anastasia_BackEnd.UnitTests.support.LenientMockitoTest;
import com.anastasia.Anastasia_BackEnd.common.i18n.LocalizedMessageService;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.OnboardingSessionStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.SubscriptionPlan;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantOnboardingSessionEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantType;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.WebhookEventReceiptEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantOnboardingSessionRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.WebhookEventReceiptRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.onboarding.OnboardingStripeWebhookService;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.onboarding.TenantOnboardingProvisioningService;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.onboarding.TenantPlanBillingCatalog;
import com.stripe.model.checkout.Session;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@LenientMockitoTest
class OnboardingStripeWebhookServiceTest {

    @Mock
    private TenantOnboardingSessionRepository onboardingSessionRepository;

    @Mock
    private WebhookEventReceiptRepository webhookEventReceiptRepository;

    @Mock
    private TenantPlanBillingCatalog billingCatalog;

    @Mock
    private TenantOnboardingProvisioningService onboardingProvisioningService;

    @Mock
    private LocalizedMessageService messageService;

    @InjectMocks
    private OnboardingStripeWebhookService service;

    @Test
    void handleCheckoutSessionCompleted_persistsSanitizedWebhookReceipt() {
        UUID sessionId = UUID.randomUUID();
        String payload = "{\"id\":\"evt_123\",\"email\":\"owner@example.com\",\"card\":\"4242424242424242\"}";
        String signatureHeader = "t=123,v1=secret-signature";
        Instant createdAt = Instant.now();

        TenantOnboardingSessionEntity sessionEntity = TenantOnboardingSessionEntity.builder()
                .id(sessionId)
                .status(OnboardingSessionStatus.DRAFT)
                .tenantType(TenantType.CHURCH)
                .selectedPlan(SubscriptionPlan.BASIC)
                .ownerName("Owner")
                .ownerEmail("owner@example.com")
                .ownerPhone("+15551234567")
                .draftPayloadJson("{}")
                .draftPasswordHash("hash")
                .expiresAt(createdAt.plusSeconds(600))
                .build();

        Session stripeSession = new Session();
        stripeSession.setId("cs_test_123");
        stripeSession.setCustomer("cus_123");
        stripeSession.setSubscription("sub_123");
        stripeSession.setMetadata(Map.of("onboardingSessionId", sessionId.toString()));

        when(webhookEventReceiptRepository.findByProviderAndEventId("STRIPE", "evt_123"))
                .thenReturn(Optional.empty());
        when(onboardingSessionRepository.findById(sessionId)).thenReturn(Optional.of(sessionEntity));
        when(webhookEventReceiptRepository.save(any(WebhookEventReceiptEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        boolean handled = service.handleCheckoutSessionCompleted(
                "evt_123",
                "checkout.session.completed",
                createdAt,
                payload,
                signatureHeader,
                stripeSession
        );

        assertThat(handled).isTrue();

        ArgumentCaptor<WebhookEventReceiptEntity> receiptCaptor =
                ArgumentCaptor.forClass(WebhookEventReceiptEntity.class);
        verify(webhookEventReceiptRepository, times(2)).save(receiptCaptor.capture());

        List<WebhookEventReceiptEntity> savedReceipts = receiptCaptor.getAllValues();
        assertThat(savedReceipts).hasSize(2);

        for (WebhookEventReceiptEntity receipt : savedReceipts) {
            assertThat(receipt.getPayload()).contains("\"payloadSize\":").contains("\"payloadSha256\":\"");
            assertThat(receipt.getPayload()).doesNotContain("owner@example.com");
            assertThat(receipt.getPayload()).doesNotContain("4242424242424242");
            assertThat(receipt.getPayload()).doesNotContain(payload);
            assertThat(receipt.getSignatureHeader()).isNull();
        }

        assertThat(savedReceipts.get(savedReceipts.size() - 1).getProcessingResult()).isNotNull();
    }
}
