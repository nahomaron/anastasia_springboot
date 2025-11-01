package com.anastasia.Anastasia_BackEnd.modules.registration.config;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.MemberEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.MemberRepository;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@ComponentScan(basePackageClasses = RegistrationModuleConfig.class)
@EntityScan(basePackageClasses = MemberEntity.class)
@EnableJpaRepositories(basePackageClasses = MemberRepository.class)
public class RegistrationModuleConfig {
}
