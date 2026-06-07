package com.anastasia.Anastasia_BackEnd.UnitTests.controller;

import com.anastasia.Anastasia_BackEnd.modules.payments.application.usecase.HandleSubscriptionWebhookUseCase;
import com.anastasia.Anastasia_BackEnd.modules.payments.application.usecase.HandleWebhookEventUseCase;
import com.anastasia.Anastasia_BackEnd.modules.payments.stripe.StripeWebhookVerifier;
import com.anastasia.Anastasia_BackEnd.modules.payments.web.controller.WebhookController;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.onboarding.OnboardingStripeWebhookService;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.onboarding.TenantSubscriptionStripeWebhookService;
import com.stripe.model.Event;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class WebhookControllerUnitTest {

    @Mock
    private StripeWebhookVerifier verifier;

    @Mock
    private HandleWebhookEventUseCase paymentHandler;

    @Mock
    private HandleSubscriptionWebhookUseCase subscriptionHandler;

    @Mock
    private OnboardingStripeWebhookService onboardingStripeWebhookService;

    @Mock
    private TenantSubscriptionStripeWebhookService tenantSubscriptionStripeWebhookService;

    @InjectMocks
    private WebhookController controller;

    @Test
    void handle_logsOnlyWebhookMetadataWithoutRawPayloadOrSignature(CapturedOutput output) throws Exception {
        String payload = "{\"email\":\"owner@example.com\",\"card\":\"4242424242424242\"}";
        String signatureHeader = "t=789,v1=super-secret-signature";

        Event event = new Event();
        event.setId("evt_789");
        event.setType("unsupported.event");

        when(verifier.verify(payload, signatureHeader)).thenReturn(event);

        ResponseEntity<Void> response = controller.handle(signatureHeader, payload);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(output.getOut()).contains("Stripe webhook received: payloadSize=" + payload.length());
        assertThat(output.getOut()).contains("signaturePresent=true");
        assertThat(output.getOut()).contains("Stripe webhook verified: id=evt_789, type=unsupported.event");
        assertThat(output.getOut()).doesNotContain(payload);
        assertThat(output.getOut()).doesNotContain("owner@example.com");
        assertThat(output.getOut()).doesNotContain("4242424242424242");
        assertThat(output.getOut()).doesNotContain(signatureHeader);
        assertThat(output.getOut()).doesNotContain("super-secret-signature");
    }
}
