package com.anastasia.Anastasia_BackEnd.core.kafka.config;

import com.anastasia.Anastasia_BackEnd.core.kafka.support.KafkaInfrastructureProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;

/**
 * Configures the shared Kafka listener container factory.
 */
@Configuration
@RequiredArgsConstructor
public class KafkaConsumerConfig {

    private final KafkaInfrastructureProperties infrastructureProperties;

    @Bean
    public ConcurrentKafkaListenerContainerFactory<Object, Object> kafkaListenerContainerFactory(
            ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
            ConsumerFactory<Object, Object> consumerFactory,
            DefaultErrorHandler errorHandler) {

        ConcurrentKafkaListenerContainerFactory<Object, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        configurer.configure(factory, consumerFactory);
        factory.setCommonErrorHandler(errorHandler);

        if (infrastructureProperties.getListenerConcurrency() != null) {
            factory.setConcurrency(infrastructureProperties.getListenerConcurrency());
        }

        return factory;
    }
}
