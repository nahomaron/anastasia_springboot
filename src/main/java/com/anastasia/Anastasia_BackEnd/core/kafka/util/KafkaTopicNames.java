package com.anastasia.Anastasia_BackEnd.core.kafka.util;

/**
 * Canonical Kafka topic names used across the platform.
 * The actual broker topics may be prefixed; prefer resolving via {@code KafkaTopicNameResolver}.
 */
public final class KafkaTopicNames {

    public static final String MEMBER_REGISTERED = "member.registered";
    public static final String CHILD_REGISTERED = "child.registered";

    public static final String USER_INVITED = "user.invited";

    public static final String EVENT_CREATED = "events.created";

    public static final String PAYMENTS_AUTHORIZED = "payments.authorized";
    public static final String PAYMENTS_CAPTURED = "payments.captured";
    public static final String PAYMENTS_EVENTS = "payments.events";
    public static final String SUBSCRIPTIONS_ACTIVATED = "subscriptions.activated";
    public static final String SUBSCRIPTIONS_CANCELED = "subscriptions.canceled";
    public static final String GENERIC_EVENTS = "generic.event";

    private KafkaTopicNames() {
    }
}
