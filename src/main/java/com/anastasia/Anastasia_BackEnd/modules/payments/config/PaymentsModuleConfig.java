package com.anastasia.Anastasia_BackEnd.modules.payments.config;

import com.anastasia.Anastasia_BackEnd.modules.payments.domain.model.PaymentIntent;
import com.anastasia.Anastasia_BackEnd.modules.payments.infrastructure.repository.PaymentIntentRepository;
import com.anastasia.Anastasia_BackEnd.modules.payments.infrastructure.repository.PaymentSubscriptionRepository;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ComponentScan(basePackageClasses = PaymentsModuleConfig.class)
@EntityScan(basePackageClasses = PaymentIntent.class)
@EnableJpaRepositories(basePackageClasses = {
        PaymentIntentRepository.class,
        PaymentSubscriptionRepository.class
})
public class PaymentsModuleConfig {
}
