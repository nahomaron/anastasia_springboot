package com.anastasia.Anastasia_BackEnd.modules.accounting.config;

import com.anastasia.Anastasia_BackEnd.modules.accounting.model.Account;
import com.anastasia.Anastasia_BackEnd.modules.accounting.repository.TransactionRepository;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Configuration class for the Accounting module.
 * We enable JPA Auditing here to automatically populate createdAt and updatedAt fields.
 */
@Configuration
@ComponentScan(basePackageClasses = AccountingModuleConfig.class)
@EntityScan(basePackageClasses = Account.class)
@EnableJpaRepositories(basePackageClasses = TransactionRepository.class)
public class AccountingModuleConfig {
    // You can define module-specific beans here if needed.
}
