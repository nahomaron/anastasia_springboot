package com.anastasia.Anastasia_BackEnd.modules.payments.infrastructure.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Value("${app.kafka.partitions:3}")
    private int partitions;

    @Value("${app.kafka.replicas:1}")
    private short replicas;

    @Bean
    public NewTopic paymentsCaptured(@Value("${spring.kafka.properties.topic.prefix:}") String topicPrefix) {
        return TopicBuilder.name(topicPrefix + "payments.captured")
                .partitions(partitions)
                .replicas(replicas)
                .build();
    }

    @Bean
    public NewTopic subscriptionsActivated() {
        return TopicBuilder.name("subscriptions.activated")
                .partitions(partitions)
                .replicas(replicas)
                .build();
    }

    @Bean
    public NewTopic subscriptionsCanceled() {
        return TopicBuilder.name("subscriptions.canceled")
                .partitions(partitions)
                .replicas(replicas)
                .build();
    }
}
