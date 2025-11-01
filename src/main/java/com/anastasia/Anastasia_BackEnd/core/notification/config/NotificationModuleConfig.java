package com.anastasia.Anastasia_BackEnd.core.notification.config;

import com.anastasia.Anastasia_BackEnd.core.notification.repository.NotificationRepository;
import com.anastasia.Anastasia_BackEnd.core.notification.template.EmailTemplateEntity;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Module entrypoint for the notification subsystem.
 * Keeps all notification-related beans behind an explicit configuration gate.
 */
@Configuration
@ComponentScan(basePackageClasses = NotificationModuleConfig.class)
@EntityScan(basePackageClasses = EmailTemplateEntity.class)
@EnableJpaRepositories(basePackageClasses = NotificationRepository.class)
public class NotificationModuleConfig {
}
