package com.anastasia.Anastasia_BackEnd.core.notification.listener;

import com.anastasia.Anastasia_BackEnd.core.kafka.util.KafkaConsumerGroupNames;
import com.anastasia.Anastasia_BackEnd.core.notification.channel.EmailNotificationService;
import com.anastasia.Anastasia_BackEnd.core.notification.template.EmailTemplateName;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentKafkaListener {

    private final EmailNotificationService emailService;

    @KafkaListener(topics = "#{@kafkaTopicNameResolver.paymentsCaptured()}", groupId = KafkaConsumerGroupNames.NOTIFICATIONS)
    public void handlePaymentCaptured(JsonNode payload) {
        try {
            String email = payload.path("memberEmail").asText(null);
            String paymentId = payload.path("paymentId").asText();
            long amount = payload.path("gross").asLong();
            String currency = payload.path("currency").asText();

            if (email == null || email.isBlank()) {
                log.warn("Missing email in payment.captured payload: {}", payload);
                return;
            }

            emailService.sendEmail(
                    email,
                    "Payment Confirmation",
                    EmailTemplateName.PAYMENT_RECEIPT,
                    Map.of(
                            "amount", amount / 100.0,
                            "currency", currency,
                            "paymentId", paymentId
                    )
            );

        } catch (Exception e) {
            log.error("Failed to handle payment.captured event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "#{@kafkaTopicNameResolver.subscriptionsActivated()}", groupId = KafkaConsumerGroupNames.NOTIFICATIONS)
    public void handleSubscriptionActivated(JsonNode payload) {
        try {
            String email = payload.path("memberEmail").asText(null);
            String subId = payload.path("subscriptionId").asText();
            String purpose = payload.path("purpose").asText();

            if (email == null || email.isBlank()) {
                log.warn("Missing email in subscriptions.activated payload: {}", payload);
                return;
            }

            emailService.sendEmail(
                    email,
                    "Subscription Activated",
                    EmailTemplateName.SUBSCRIPTION_ACTIVATED,
                    Map.of("subscriptionId", subId, "purpose", purpose)
            );

        } catch (Exception e) {
            log.error("Failed to handle subscriptions.activated event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "#{@kafkaTopicNameResolver.subscriptionsCanceled()}", groupId = KafkaConsumerGroupNames.NOTIFICATIONS)
    public void handleSubscriptionCanceled(JsonNode payload) {
        try {
            String email = payload.path("memberEmail").asText(null);
            String subId = payload.path("subscriptionId").asText();

            if (email == null || email.isBlank()) {
                log.warn("Missing email in subscriptions.canceled payload: {}", payload);
                return;
            }

            emailService.sendEmail(
                    email,
                    "Subscription Canceled",
                    EmailTemplateName.SUBSCRIPTION_CANCELED,
                    Map.of("subscriptionId", subId)
            );

        } catch (Exception e) {
            log.error("Failed to handle subscriptions.canceled event: {}", e.getMessage(), e);
        }
    }
}
