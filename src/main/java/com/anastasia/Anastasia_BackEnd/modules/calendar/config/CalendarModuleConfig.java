package com.anastasia.Anastasia_BackEnd.modules.calendar.config;

import com.anastasia.Anastasia_BackEnd.modules.calendar.repository.CalendarEntryRepository;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackageClasses = CalendarEntryRepository.class)
public class CalendarModuleConfig {
}
