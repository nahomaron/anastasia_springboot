package com.anastasia.Anastasia_BackEnd.modules.payments.application.usecase;

import com.anastasia.Anastasia_BackEnd.modules.payments.domain.events.PaymentEventType;
import com.anastasia.Anastasia_BackEnd.modules.payments.domain.model.PaymentSubscription;
import com.anastasia.Anastasia_BackEnd.modules.payments.domain.model.SubscriptionStatus;
import com.anastasia.Anastasia_BackEnd.modules.payments.infrastructure.outbox.OutboxPublisher;
import com.anastasia.Anastasia_BackEnd.modules.payments.infrastructure.repository.PaymentSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class HandleSubscriptionWebhookUseCase {

    private final PaymentSubscriptionRepository subscriptionRepository;
    private final OutboxPublisher outboxPublisher;

    @Transactional
    public void handleSubscriptionActivated(UUID subscriptionId, String providerSubscriptionId) {
        var subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown payment subscription: " + subscriptionId));

        if (!"STRIPE".equalsIgnoreCase(subscription.getProvider())) {
            throw new IllegalStateException("Unsupported provider for subscription " + subscriptionId);
        }

        boolean alreadyActive = subscription.getProviderRef() != null
                && subscription.getProviderRef().equals(providerSubscriptionId)
                && subscription.getStatus() == SubscriptionStatus.ACTIVE;
        if (alreadyActive) {
            log.debug("Subscription {} already active with provider ref {}", subscriptionId, providerSubscriptionId);
            return;
        }

        subscription.markActive(providerSubscriptionId);
        subscriptionRepository.save(subscription);

        outboxPublisher.publish(
                PaymentEventType.SUBSCRIPTION_ACTIVATED,
                subscription.getId().toString(),
                subscription.getTenantId(),
                Map.of(
                        "subscriptionId", subscription.getId().toString(),
                        "providerRef", subscription.getProviderRef(),
                        "status", subscription.getStatus().name()
                )
        );
    }

    @Transactional
    public void handleSubscriptionCanceled(UUID subscriptionId) {
        var subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown payment subscription: " + subscriptionId));

        if (!"STRIPE".equalsIgnoreCase(subscription.getProvider())) {
            throw new IllegalStateException("Unsupported provider for subscription " + subscriptionId);
        }

        if (subscription.getStatus() == SubscriptionStatus.CANCELED) {
            log.debug("Subscription {} already canceled", subscriptionId);
            return;
        }

        subscription.markCanceled();
        subscriptionRepository.save(subscription);

        outboxPublisher.publish(
                PaymentEventType.SUBSCRIPTION_CANCELED,
                subscription.getId().toString(),
                subscription.getTenantId(),
                Map.of(
                        "subscriptionId", subscription.getId().toString(),
                        "status", subscription.getStatus().name()
                )
        );
    }
}
