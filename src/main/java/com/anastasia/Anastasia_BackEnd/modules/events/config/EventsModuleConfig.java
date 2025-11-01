package com.anastasia.Anastasia_BackEnd.modules.events.config;

import com.anastasia.Anastasia_BackEnd.modules.events.model.EventEntity;
import com.anastasia.Anastasia_BackEnd.modules.events.repository.EventRepository;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@ComponentScan(basePackageClasses = EventsModuleConfig.class)
@EntityScan(basePackageClasses = EventEntity.class)
@EnableJpaRepositories(basePackageClasses = EventRepository.class)
public class EventsModuleConfig {
}
