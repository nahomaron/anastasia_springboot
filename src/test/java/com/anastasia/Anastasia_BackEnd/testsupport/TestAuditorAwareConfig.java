package com.anastasia.Anastasia_BackEnd.testsupport;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;

import java.util.Optional;
import java.util.UUID;

@TestConfiguration
public class TestAuditorAwareConfig {

    @Bean
    public AuditorAware<UUID> auditorAware(){
        return () -> Optional.of(UUID.randomUUID());
    }
}
