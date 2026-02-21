package com.anastasia.Anastasia_BackEnd.core.kafka.config;

import com.anastasia.Anastasia_BackEnd.core.kafka.support.KafkaInfrastructureProperties;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Shared error handler that retries and then publishes failures to a dead-letter topic.
 */
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaErrorHandlerConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaErrorHandlerConfig.class);
    private static final FixedBackOff RETRY_BACKOFF = new FixedBackOff(5000L, 3L);

    private final KafkaInfrastructureProperties properties;

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(ObjectProvider<KafkaOperations<?, ?>> kafkaOperationsProvider) {
        KafkaOperations<?, ?> kafkaOperations = kafkaOperationsProvider.getIfAvailable();
        DefaultErrorHandler errorHandler;

        if (kafkaOperations == null) {
            // Allow test/local profiles without producer wiring to boot.
            errorHandler = new DefaultErrorHandler(RETRY_BACKOFF);
        } else {
            DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                    kafkaOperations,
                    (record, ex) -> new TopicPartition(deadLetterTopic(record), record.partition())
            );
            errorHandler = new DefaultErrorHandler(recoverer, RETRY_BACKOFF);
        }

        errorHandler.setRetryListeners(this::logRetryAttempt);
        return errorHandler;
    }

    private String deadLetterTopic(ConsumerRecord<?, ?> record) {
        return properties.deadLetterTopic(record.topic());
    }

    private void logRetryAttempt(ConsumerRecord<?, ?> record, Exception exception, int deliveryAttempt) {
        log.warn("Retrying Kafka record topic={} key={} attempt={} due to={}",
                record.topic(),
                record.key(),
                deliveryAttempt,
                exception.getMessage());
    }
}
