package com.anastasia.Anastasia_BackEnd.modules.payments.infrastructure.config;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaErrorHandlerConfig {

    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<Object, Object> template) {
        // 3 retries, 5 sec apart, then send to dead letter topic
        var recoverer = new DeadLetterPublishingRecoverer(template,
                (cr, e) -> new org.apache.kafka.common.TopicPartition(cr.topic() + ".DLT", cr.partition()));

        var backOff = new FixedBackOff(5000L, 3L);
        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);

        handler.setRetryListeners((record, ex, deliveryAttempt) ->
                System.err.printf("Retrying record %s due to %s (attempt %d)%n",
                        record.value(), ex.getMessage(), deliveryAttempt));

        return handler;
    }
}
