package com.anastasia.Anastasia_BackEnd.modules.groups.config;

import com.anastasia.Anastasia_BackEnd.modules.groups.GroupRepository;
import com.anastasia.Anastasia_BackEnd.modules.groups.model.GroupEntity;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@ComponentScan(basePackageClasses = GroupsModuleConfig.class)
@EntityScan(basePackageClasses = GroupEntity.class)
@EnableJpaRepositories(basePackageClasses = GroupRepository.class)
public class GroupsModuleConfig {
}
