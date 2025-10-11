package com.anastasia.Anastasia_BackEnd.config;

import com.anastasia.Anastasia_BackEnd.auditing.ApplicationAuditAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.AuditorAware;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;

@Configuration
public class ApplicationConfig {

//    @Bean
//    public AuditorAware<UUID> auditorAware(){
//        return new ApplicationAuditAware();
//    }

    @Bean
    public AuditorAware<UUID> auditorAware(Environment environment) {
        if (isTestProfile(environment)) {
            // Test profile: return dummy auditor
            return () -> Optional.of(UUID.randomUUID());
        } else {
            // Production: return actual implementation
            return new ApplicationAuditAware();
        }
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
        for (String profile : environment.getActiveProfiles()) {
            if (profile.equalsIgnoreCase("test")) {
                return true;
            }
        }
        return false;
    }
}
