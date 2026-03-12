package com.anastasia.Anastasia_BackEnd.modules.payments.application.usecase;

import com.anastasia.Anastasia_BackEnd.common.i18n.LocalizedMessageService;
import com.anastasia.Anastasia_BackEnd.modules.payments.domain.events.PaymentEventType;
import com.anastasia.Anastasia_BackEnd.modules.payments.domain.model.SubscriptionStatus;
import com.anastasia.Anastasia_BackEnd.core.outbox.OutboxPublisher;
import com.anastasia.Anastasia_BackEnd.modules.payments.repository.PaymentSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class HandleSubscriptionWebhookUseCase {

    private final PaymentSubscriptionRepository subscriptionRepository;
    private final OutboxPublisher outboxPublisher;
    private final LocalizedMessageService messageService;

    @Transactional
    public void handleSubscriptionActivated(UUID subscriptionId, String providerSubscriptionId) {
        handleSubscriptionActivated(subscriptionId, providerSubscriptionId, null, null, Instant.now());
    }

    @Transactional
    public void handleSubscriptionActivated(UUID subscriptionId,
                                            String providerSubscriptionId,
                                            String providerEventId,
                                            String providerEventType,
                                            Instant occurredAt) {
        var subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException(messageService.get(
                        "payments.subscription.unknown",
                        "Unknown payment subscription: {0}",
                        subscriptionId
                )));

        if (!"STRIPE".equalsIgnoreCase(subscription.getProvider())) {
            throw new IllegalStateException(messageService.get(
                    "payments.subscription.provider.unsupported",
                    "Unsupported provider for subscription {0}",
                    subscriptionId
            ));
        }

        boolean alreadyActive = subscription.getProviderSubscriptionReference() != null
                && subscription.getProviderSubscriptionReference().equals(providerSubscriptionId)
                && subscription.getStatus() == SubscriptionStatus.ACTIVE;
        if (alreadyActive) {
            log.debug("Subscription {} already active with provider ref {}", subscriptionId, providerSubscriptionId);
            return;
        }

        subscription.recordProviderEvent(providerEventId, providerEventType, occurredAt);
        subscription.markActive(providerSubscriptionId);
        subscriptionRepository.save(subscription);

        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("subscriptionId", subscription.getId().toString());
        payload.put("providerSubscriptionReference", subscription.getProviderSubscriptionReference());
        payload.put("status", subscription.getStatus().name());
        payload.put("providerEventId", subscription.getLastProviderEventId());
        payload.put("providerEventType", subscription.getLastProviderEventType());
        payload.put("memberId", subscription.getMemberId());
        payload.put("userId", subscription.getUserId() != null ? subscription.getUserId().toString() : null);
        payload.put("userEmail", subscription.getUserEmail());
        payload.put("memberEmail", subscription.getUserEmail());

        outboxPublisher.publish(
                PaymentEventType.SUBSCRIPTION_ACTIVATED,
                subscription.getTenantId(),
                subscription.getId().toString(),
                payload
        );
    }

    @Transactional
    public void handleSubscriptionCanceled(UUID subscriptionId) {
        handleSubscriptionCanceled(subscriptionId, null, null, Instant.now());
    }

    @Transactional
    public void handleSubscriptionCanceled(UUID subscriptionId,
                                           String providerEventId,
                                           String providerEventType,
                                           Instant occurredAt) {
        var subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException(messageService.get(
                        "payments.subscription.unknown",
                        "Unknown payment subscription: {0}",
                        subscriptionId
                )));

        if (!"STRIPE".equalsIgnoreCase(subscription.getProvider())) {
            throw new IllegalStateException(messageService.get(
                    "payments.subscription.provider.unsupported",
                    "Unsupported provider for subscription {0}",
                    subscriptionId
            ));
        }

        if (subscription.getStatus() == SubscriptionStatus.CANCELED) {
            log.debug("Subscription {} already canceled", subscriptionId);
            return;
        }

        subscription.recordProviderEvent(providerEventId, providerEventType, occurredAt);
        subscription.markCanceled(providerEventType);
        subscriptionRepository.save(subscription);

        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("subscriptionId", subscription.getId().toString());
        payload.put("status", subscription.getStatus().name());
        payload.put("providerEventId", subscription.getLastProviderEventId());
        payload.put("providerEventType", subscription.getLastProviderEventType());
        payload.put("memberId", subscription.getMemberId());
        payload.put("userId", subscription.getUserId() != null ? subscription.getUserId().toString() : null);
        payload.put("userEmail", subscription.getUserEmail());
        payload.put("memberEmail", subscription.getUserEmail());

        outboxPublisher.publish(
                PaymentEventType.SUBSCRIPTION_CANCELED,
                subscription.getTenantId(),
                subscription.getId().toString(),
                payload
        );
    }
}
