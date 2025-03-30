package com.anastasia.Anastasia_BackEnd.config;

import com.anastasia.Anastasia_BackEnd.auditing.ApplicationAuditAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;

import java.util.UUID;

@Configuration
public class ApplicationConfig {

    @Bean
    public AuditorAware<UUID> auditorAware(){
        return new ApplicationAuditAware();
    }
}
