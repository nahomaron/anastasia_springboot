package com.anastasia.Anastasia_BackEnd.core.kafka.config;

import com.anastasia.Anastasia_BackEnd.core.kafka.support.KafkaInfrastructureProperties;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Shared error handler that retries and then publishes failures to a dead-letter topic.
 */
@Configuration
@RequiredArgsConstructor
public class KafkaErrorHandlerConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaErrorHandlerConfig.class);

    private final KafkaInfrastructureProperties properties;

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<Object, Object> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, ex) -> new TopicPartition(deadLetterTopic(record), record.partition())
        );

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, new FixedBackOff(5000L, 3L));
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
