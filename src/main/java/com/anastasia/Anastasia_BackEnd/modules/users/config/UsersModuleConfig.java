package com.anastasia.Anastasia_BackEnd.modules.users.config;

import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = "com.anastasia.Anastasia_BackEnd.modules.users")
@EntityScan(basePackageClasses = UserEntity.class)
public class UsersModuleConfig {
}
