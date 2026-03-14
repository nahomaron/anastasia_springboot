package com.anastasia.Anastasia_BackEnd.modules.staff.config;

import com.anastasia.Anastasia_BackEnd.modules.staff.model.StaffEntity;
import com.anastasia.Anastasia_BackEnd.modules.staff.repository.StaffRepository;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@ComponentScan(basePackages = "com.anastasia.Anastasia_BackEnd.modules.staff")
@EntityScan(basePackageClasses = StaffEntity.class)
@EnableJpaRepositories(basePackageClasses = StaffRepository.class)
public class StaffModuleConfig {
}
