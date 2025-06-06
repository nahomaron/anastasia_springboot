package com.anastasia.Anastasia_BackEnd.config;

import com.anastasia.Anastasia_BackEnd.auditing.ApplicationAuditAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.AuditorAware;

import java.util.Optional;
import java.util.UUID;

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


    private boolean isTestProfile(Environment environment) {
        for (String profile : environment.getActiveProfiles()) {
            if (profile.equalsIgnoreCase("test")) {
                return true;
            }
        }
        return false;
    }
}
