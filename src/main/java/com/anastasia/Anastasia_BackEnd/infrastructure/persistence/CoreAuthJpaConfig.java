package com.anastasia.Anastasia_BackEnd.infrastructure.persistence;

import com.anastasia.Anastasia_BackEnd.core.auth.audit.PlatformAdminBootstrapAuditRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackageClasses = {
        UserRepository.class,
        PlatformAdminBootstrapAuditRepository.class
})
@EntityScan(basePackages = "com.anastasia.Anastasia_BackEnd.core.auth")
public class CoreAuthJpaConfig {
}
