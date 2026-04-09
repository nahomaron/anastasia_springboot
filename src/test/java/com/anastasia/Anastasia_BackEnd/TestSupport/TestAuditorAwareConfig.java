package com.anastasia.Anastasia_BackEnd.TestSupport;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.AuditorAware;

import java.util.Optional;
import java.util.UUID;

@TestConfiguration
@Profile("test")
public class TestAuditorAwareConfig {

    @Bean
    public AuditorAware<UUID> auditorAware(){
        return () -> Optional.of(UUID.randomUUID());
    }
}
