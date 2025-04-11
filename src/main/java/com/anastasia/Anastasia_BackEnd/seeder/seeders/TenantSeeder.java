package com.anastasia.Anastasia_BackEnd.seeder.seeders;

import com.anastasia.Anastasia_BackEnd.model.tenant.SubscriptionPlan;
import com.anastasia.Anastasia_BackEnd.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.model.tenant.TenantType;
import com.anastasia.Anastasia_BackEnd.repository.TenantRepository;
import com.anastasia.Anastasia_BackEnd.seeder.DataSeeder;
import lombok.RequiredArgsConstructor;
import net.datafaker.Faker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TenantSeeder {
    private static final Logger logger = LoggerFactory.getLogger(TenantSeeder.class);  //  Use SLF4J logger

    private final TenantRepository tenantRepository;

    public List<TenantEntity> seedTenants(){
        if(tenantRepository.count() == 0){

            Faker faker = new Faker();

            List<TenantEntity> tenants = new ArrayList<>();

            for (int i = 0; i < 20; i++) {

                TenantEntity tenant = TenantEntity.builder()
                        .ownerName(faker.name().name())
                        .phoneNumber(faker.phoneNumber().phoneNumberInternational())
                        .tenantType(faker.options().option(TenantType.CHURCH))
                        .subscriptionPlan(faker.options().option(SubscriptionPlan.BASIC, SubscriptionPlan.FREE, SubscriptionPlan.ENTERPRISE))
                        .build();
                tenants.add(tenant);

            }

            List<TenantEntity> savedTenants = tenantRepository.saveAll(tenants);
            logger.info("Seeded {} fake tenants", savedTenants.size());
            return savedTenants;
        }
        return Collections.emptyList();
    }
}
