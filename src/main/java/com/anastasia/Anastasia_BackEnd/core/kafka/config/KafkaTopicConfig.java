package com.anastasia.Anastasia_BackEnd.core.kafka.config;

import com.anastasia.Anastasia_BackEnd.core.kafka.support.KafkaInfrastructureProperties;
import com.anastasia.Anastasia_BackEnd.core.kafka.support.KafkaTopicNameResolver;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Declares Kafka topics that are shared across modules.
 */
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaTopicConfig {

    private final KafkaInfrastructureProperties properties;
    private final KafkaTopicNameResolver topicNameResolver;

    @Bean
    public NewTopic memberRegistered(){return buildTopic(topicNameResolver.memberRegistered());}

    @Bean
    public NewTopic paymentsAuthorizedTopic() {
        return buildTopic(topicNameResolver.paymentsAuthorized());
    }

    @Bean
    public NewTopic paymentsCapturedTopic() {
        return buildTopic(topicNameResolver.paymentsCaptured());
    }

    @Bean
    public NewTopic paymentsEventsTopic() {
        return buildTopic(topicNameResolver.paymentsEvents());
    }

    @Bean
    public NewTopic subscriptionsActivatedTopic() {
        return buildTopic(topicNameResolver.subscriptionsActivated());
    }

    @Bean
    public NewTopic subscriptionsCanceledTopic() {
        return buildTopic(topicNameResolver.subscriptionsCanceled());
    }

    @Bean
    public NewTopic paymentsCapturedDeadLetterTopic() {
        return buildTopic(topicNameResolver.deadLetterFor(topicNameResolver.paymentsCaptured()));
    }

    @Bean
    public NewTopic subscriptionsActivatedDeadLetterTopic() {
        return buildTopic(topicNameResolver.deadLetterFor(topicNameResolver.subscriptionsActivated()));
    }

    @Bean
    public NewTopic subscriptionsCanceledDeadLetterTopic() {
        return buildTopic(topicNameResolver.deadLetterFor(topicNameResolver.subscriptionsCanceled()));
    }

    private NewTopic buildTopic(String topicName) {
        return TopicBuilder
                .name(topicName)
                .partitions(properties.getPartitions())
                .replicas(properties.getReplicas())
                .build();
    }
}
