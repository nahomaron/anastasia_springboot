package com.anastasia.Anastasia_BackEnd.modules.kafka.util;

/**
 * Shared consumer group identifiers for Kafka listeners.
 */
public final class KafkaConsumerGroupNames {

    public static final String NOTIFICATIONS = "anastasia-notifications";
    public static final String ACCOUNTING = "anastasia-accounting";

    private KafkaConsumerGroupNames() {
    }
}
