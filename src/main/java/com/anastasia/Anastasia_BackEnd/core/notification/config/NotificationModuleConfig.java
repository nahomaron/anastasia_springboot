package com.anastasia.Anastasia_BackEnd.core.notification.config;

import com.anastasia.Anastasia_BackEnd.core.notification.repository.NotificationRepository;
import com.anastasia.Anastasia_BackEnd.core.notification.template.EmailTemplateEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.web.client.RestOperations;

/**
 * Module entrypoint for the notification subsystem.
 * Keeps all notification-related beans behind an explicit configuration gate.
 */
@Configuration
@ComponentScan(basePackageClasses = NotificationModuleConfig.class)
@EntityScan(basePackageClasses = {EmailTemplateEntity.class, TenantEntity.class})
@EnableJpaRepositories(basePackageClasses = NotificationRepository.class)
public class NotificationModuleConfig {

    @Bean
    public RestOperations snsWebhookRestOperations(RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder.build();
    }
}
