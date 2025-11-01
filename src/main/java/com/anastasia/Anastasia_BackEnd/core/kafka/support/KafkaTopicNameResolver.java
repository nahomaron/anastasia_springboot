package com.anastasia.Anastasia_BackEnd.core.kafka.support;

import com.anastasia.Anastasia_BackEnd.core.kafka.util.KafkaTopicNames;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Resolves the concrete topic names (including prefixes) for publishers and listeners.
 */
@Component
@RequiredArgsConstructor
public class KafkaTopicNameResolver {

    private final KafkaInfrastructureProperties properties;

    public String paymentsAuthorized() {
        return properties.applyPrefix(KafkaTopicNames.PAYMENTS_AUTHORIZED);
    }

    public String paymentsCaptured() {
        return properties.applyPrefix(KafkaTopicNames.PAYMENTS_CAPTURED);
    }

    public String paymentsEvents() {
        return properties.applyPrefix(KafkaTopicNames.PAYMENTS_EVENTS);
    }

    public String subscriptionsActivated() {
        return properties.applyPrefix(KafkaTopicNames.SUBSCRIPTIONS_ACTIVATED);
    }

    public String subscriptionsCanceled() {
        return properties.applyPrefix(KafkaTopicNames.SUBSCRIPTIONS_CANCELED);
    }

    public String deadLetterFor(String topic) {
        return properties.deadLetterTopic(topic);
    }
}
