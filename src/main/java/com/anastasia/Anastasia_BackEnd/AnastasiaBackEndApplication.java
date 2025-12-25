package com.anastasia.Anastasia_BackEnd;

import com.anastasia.Anastasia_BackEnd.modules.ModulesBootstrapConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(
		scanBasePackages = {
				"com.anastasia.Anastasia_BackEnd.core",
				"com.anastasia.Anastasia_BackEnd.common",
				"com.anastasia.Anastasia_BackEnd.infrastructure",
				"com.anastasia.Anastasia_BackEnd.seeder"
		}
)
//@SpringBootApplication
@Import(ModulesBootstrapConfiguration.class)
@EnableScheduling
@EnableAsync
@EnableRetry
@EnableCaching
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
@EnableSpringDataWebSupport
@EntityScan(basePackages = "com.anastasia.Anastasia_BackEnd")
public class AnastasiaBackEndApplication {

	public static void main(String[] args) {
		SpringApplication.run(AnastasiaBackEndApplication.class, args);
	}

}
