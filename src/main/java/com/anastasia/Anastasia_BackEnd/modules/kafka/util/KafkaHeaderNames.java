package com.anastasia.Anastasia_BackEnd.modules.kafka.util;

/**
 * Kafka record header keys shared between producers and consumers.
 */
public final class KafkaHeaderNames {

    public static final String TENANT_ID = "tenantId";
    public static final String EVENT_TYPE = "type";

    private KafkaHeaderNames() {
    }
}
