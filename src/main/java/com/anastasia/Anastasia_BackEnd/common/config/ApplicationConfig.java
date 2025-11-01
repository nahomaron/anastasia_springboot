package com.anastasia.Anastasia_BackEnd.common.config;

import com.anastasia.Anastasia_BackEnd.common.auditing.ApplicationAuditAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.AuditorAware;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;

@Configuration
public class ApplicationConfig {

    private static final UUID TEST_AUDITOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

//    @Bean
//    @Profile({"prod", "dev", "test-server"})
//    public AuditorAware<UUID> auditorAware(){
//        return new ApplicationAuditAware();
//    }

    @Bean
    public AuditorAware<UUID> auditorAware(Environment environment) {
        if (isTestProfile(environment)) {
            return () -> Optional.of(TEST_AUDITOR_ID);
        }
        return new ApplicationAuditAware();
    }

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);  // Change based on expected load
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("AsyncExecutor-");
        executor.initialize();
        return executor;
    }


    private boolean isTestProfile(Environment environment) {
        return Arrays.stream(environment.getActiveProfiles())
                .map(String::toLowerCase)
                .anyMatch(profile -> profile.contains("test"));
    }
}
