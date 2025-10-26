package com.anastasia.Anastasia_BackEnd.modules.notification.listener;

import com.anastasia.Anastasia_BackEnd.modules.notification.channel.EmailNotificationService;
import com.anastasia.Anastasia_BackEnd.service.email.EmailTemplateName;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentKafkaListener {

    private final EmailNotificationService emailService;

    @KafkaListener(topics = {"payments.captured"}, groupId = "anastasia-notifications")
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

    @KafkaListener(topics = {"subscriptions.activated"}, groupId = "anastasia-notifications")
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

    @KafkaListener(topics = {"subscriptions.canceled"}, groupId = "anastasia-notifications")
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
