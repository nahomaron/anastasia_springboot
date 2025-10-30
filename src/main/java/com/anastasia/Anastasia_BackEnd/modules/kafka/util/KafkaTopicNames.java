package com.anastasia.Anastasia_BackEnd.modules.kafka.util;

/**
 * Canonical Kafka topic names used across the platform.
 * The actual broker topics may be prefixed; prefer resolving via {@code KafkaTopicNameResolver}.
 */
public final class KafkaTopicNames {

    public static final String PAYMENTS_AUTHORIZED = "payments.authorized";
    public static final String PAYMENTS_CAPTURED = "payments.captured";
    public static final String PAYMENTS_EVENTS = "payments.events";
    public static final String SUBSCRIPTIONS_ACTIVATED = "subscriptions.activated";
    public static final String SUBSCRIPTIONS_CANCELED = "subscriptions.canceled";

    private KafkaTopicNames() {
    }
}
