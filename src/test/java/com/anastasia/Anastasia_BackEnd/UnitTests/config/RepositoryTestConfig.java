package com.anastasia.Anastasia_BackEnd.UnitTests.config;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootConfiguration
@EnableJpaRepositories(basePackages = "com.anastasia.Anastasia_BackEnd")
@EntityScan(basePackages = "com.anastasia.Anastasia_BackEnd")
@Import(com.anastasia.Anastasia_BackEnd.TestSupport.TestAuditorAwareConfig.class)
@ImportAutoConfiguration(exclude = FlywayAutoConfiguration.class)
public class RepositoryTestConfig {
}
