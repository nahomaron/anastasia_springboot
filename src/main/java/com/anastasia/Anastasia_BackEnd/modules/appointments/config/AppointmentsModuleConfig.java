package com.anastasia.Anastasia_BackEnd.modules.appointments.config;

import com.anastasia.Anastasia_BackEnd.modules.appointments.model.AppointmentEntity;
import com.anastasia.Anastasia_BackEnd.modules.appointments.repository.AppointmentRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@ComponentScan(basePackages = "com.anastasia.Anastasia_BackEnd.modules.appointments")
@EntityScan(basePackageClasses = {AppointmentEntity.class, ChurchEntity.class})
@EnableJpaRepositories(basePackageClasses = AppointmentRepository.class)
public class AppointmentsModuleConfig {
}
