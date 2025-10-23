package com.anastasia.Anastasia_BackEnd.modules.payments;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ComponentScan(basePackages = "com.anastasia.Anastasia_BackEnd.modules.payments")
public class PaymentsModule {
}
