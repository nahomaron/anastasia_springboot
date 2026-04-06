package com.anastasia.Anastasia_BackEnd.modules.services.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@ComponentScan(basePackages = "com.anastasia.Anastasia_BackEnd.modules.services")
@EntityScan(basePackages = {
        "com.anastasia.Anastasia_BackEnd.modules.services.model",
        "com.anastasia.Anastasia_BackEnd.modules.services.marriage.model"
})
@EnableJpaRepositories(basePackages = {
        "com.anastasia.Anastasia_BackEnd.modules.services.repository",
        "com.anastasia.Anastasia_BackEnd.modules.services.marriage.repository"
})
public class ServicesModuleConfig {
}
