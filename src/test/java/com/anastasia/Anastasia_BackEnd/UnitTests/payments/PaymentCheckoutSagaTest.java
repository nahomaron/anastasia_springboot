package com.anastasia.Anastasia_BackEnd.UnitTests.payments;

import com.anastasia.Anastasia_BackEnd.core.outbox.OutboxPublisher;
import com.anastasia.Anastasia_BackEnd.modules.payments.application.saga.PaymentCheckoutSaga;
import com.anastasia.Anastasia_BackEnd.modules.payments.application.usecase.CreatePaymentIntentUseCase;
import com.anastasia.Anastasia_BackEnd.modules.payments.domain.events.PaymentEventType;
import com.anastasia.Anastasia_BackEnd.modules.payments.domain.model.PaymentIntent;
import com.anastasia.Anastasia_BackEnd.modules.payments.domain.model.PaymentPurpose;
import com.anastasia.Anastasia_BackEnd.modules.payments.repository.PaymentIntentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentCheckoutSagaTest {

    @Mock
    private PaymentIntentRepository intentRepository;

    @Mock
    private CreatePaymentIntentUseCase createIntentUseCase;

    @Mock
    private OutboxPublisher outboxPublisher;

    @InjectMocks
    private PaymentCheckoutSaga saga;

    @Test
    void startCheckoutPublishesNormalizedInitiatedPayload() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        PaymentIntent intent = PaymentIntent.newInitiated(
                tenantId,
                PaymentPurpose.DONATION,
                12_500L,
                "USD",
                42L,
                userId,
                "member@example.com",
                "fund-1",
                "idem-1"
        );

        when(intentRepository.findByTenantIdAndIdempotencyKey(tenantId, "idem-1")).thenReturn(Optional.empty());
        when(createIntentUseCase.execute(
                tenantId,
                PaymentPurpose.DONATION,
                12_500L,
                "USD",
                42L,
                userId,
                "member@example.com",
                "fund-1",
                "idem-1"
        )).thenReturn(intent);

        saga.startCheckout(
                tenantId,
                PaymentPurpose.DONATION,
                12_500L,
                "USD",
                42L,
                userId,
                "member@example.com",
                "fund-1",
                "idem-1"
        );

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(outboxPublisher).publish(
                eq(PaymentEventType.PAYMENT_INITIATED),
                eq(tenantId),
                eq(intent.getId().toString()),
                payloadCaptor.capture()
        );

        assertThat(payloadCaptor.getValue()).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) payloadCaptor.getValue();
        assertThat(payload)
                .containsEntry("paymentId", intent.getId().toString())
                .containsEntry("tenantId", tenantId.toString())
                .containsEntry("status", intent.getStatus().name())
                .containsEntry("purpose", PaymentPurpose.DONATION.name())
                .containsEntry("amountMinor", 12_500L)
                .containsEntry("currency", "USD")
                .containsEntry("fundId", "fund-1")
                .containsEntry("memberId", 42L)
                .containsEntry("userId", userId.toString())
                .containsEntry("userEmail", "member@example.com");
    }
}
