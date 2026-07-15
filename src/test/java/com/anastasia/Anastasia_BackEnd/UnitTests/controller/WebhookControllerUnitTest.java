package com.anastasia.Anastasia_BackEnd.UnitTests.controller;

import com.anastasia.Anastasia_BackEnd.modules.payments.application.usecase.HandleSubscriptionWebhookUseCase;
import com.anastasia.Anastasia_BackEnd.modules.payments.application.usecase.HandleWebhookEventUseCase;
import com.anastasia.Anastasia_BackEnd.modules.payments.stripe.StripeClient;
import com.anastasia.Anastasia_BackEnd.modules.payments.stripe.StripeWebhookVerifier;
import com.anastasia.Anastasia_BackEnd.modules.payments.web.controller.WebhookController;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.onboarding.OnboardingStripeWebhookService;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.onboarding.TenantSubscriptionStripeWebhookService;
import com.google.gson.JsonParser;
import com.stripe.Stripe;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
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

    @Mock
    private StripeClient stripeClient;

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

    @Test
    void handle_paymentSucceededUsesStripeBalanceTransactionAmounts() throws Exception {
        UUID paymentId = UUID.randomUUID();
        Event event = paymentIntentSucceededEvent(paymentId, "pi_123", 10_000L, "usd", 1_729_000_000L);
        when(verifier.verify("{}", "sig")).thenReturn(event);
        when(stripeClient.retrieveCapturedChargeAmounts(any(PaymentIntent.class), eq(10_000L)))
                .thenReturn(new StripeClient.CapturedChargeAmounts(10_000L, 321L, 9_679L));

        ResponseEntity<Void> response = controller.handle("sig", "{}");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(paymentHandler).handleCaptured(
                eq(paymentId),
                eq("pi_123"),
                eq(10_000L),
                eq(321L),
                eq(9_679L),
                eq("usd"),
                eq("evt_payment_succeeded"),
                eq("payment_intent.succeeded"),
                eq(Instant.ofEpochSecond(1_729_000_000L))
        );
    }

    private Event paymentIntentSucceededEvent(UUID paymentId,
                                              String paymentIntentId,
                                              long amountReceived,
                                              String currency,
                                              long createdAt) {
        String objectJson = """
                {
                  "id": "%s",
                  "object": "payment_intent",
                  "amount": %d,
                  "amount_received": %d,
                  "currency": "%s",
                  "latest_charge": "ch_123",
                  "metadata": {
                    "paymentId": "%s"
                  }
                }
                """.formatted(paymentIntentId, amountReceived, amountReceived, currency, paymentId);

        Event.Data data = new Event.Data();
        data.setObject(JsonParser.parseString(objectJson).getAsJsonObject());

        Event event = new Event();
        event.setId("evt_payment_succeeded");
        event.setType("payment_intent.succeeded");
        event.setApiVersion(Stripe.API_VERSION);
        event.setCreated(createdAt);
        event.setData(data);
        return event;
    }
}
