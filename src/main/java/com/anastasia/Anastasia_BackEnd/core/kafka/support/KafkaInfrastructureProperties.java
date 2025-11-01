package com.anastasia.Anastasia_BackEnd.core.kafka.support;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Centralised Kafka parameters that are shared across producer and consumer configuration.
 */
@Getter
@Component
public class KafkaInfrastructureProperties {

    private final int partitions;
    private final short replicas;
    private final String topicPrefix;
    private final String deadLetterSuffix;
    private final Integer listenerConcurrency;

    public KafkaInfrastructureProperties(
            @Value("${app.kafka.partitions:3}") int partitions,
            @Value("${app.kafka.replicas:1}") short replicas,
            @Value("${app.kafka.topic-prefix:}") String explicitPrefix,
            @Value("${spring.kafka.properties.topic.prefix:}") String springKafkaPrefix,
            @Value("${app.kafka.dead-letter-suffix:.DLT}") String deadLetterSuffix,
            @Value("${app.kafka.listener-concurrency:#{null}}") Integer listenerConcurrency
    ) {
        this.partitions = partitions;
        this.replicas = replicas;
        this.deadLetterSuffix = deadLetterSuffix;
        this.listenerConcurrency = listenerConcurrency;

        String prefixCandidate = !explicitPrefix.isBlank() ? explicitPrefix : springKafkaPrefix;
        this.topicPrefix = prefixCandidate == null ? "" : prefixCandidate;
    }

    /**
     * Apply the configured topic prefix to the canonical topic name.
     */
    public String applyPrefix(String canonicalTopic) {
        if (topicPrefix == null || topicPrefix.isBlank()) {
            return canonicalTopic;
        }
        return topicPrefix + canonicalTopic;
    }

    /**
     * Append the configured dead letter suffix to the provided topic.
     */
    public String deadLetterTopic(String topicName) {
        return topicName + deadLetterSuffix;
    }
}
