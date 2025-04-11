package com.anastasia.Anastasia_BackEnd.seeder.seeders;

import com.anastasia.Anastasia_BackEnd.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.model.common.Address;
import com.anastasia.Anastasia_BackEnd.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.model.user.UserEntity;
import com.anastasia.Anastasia_BackEnd.repository.ChurchRepository;
import com.anastasia.Anastasia_BackEnd.repository.TenantRepository;
import com.anastasia.Anastasia_BackEnd.seeder.DataSeeder;
import com.anastasia.Anastasia_BackEnd.service.registration.TenantService;
import com.anastasia.Anastasia_BackEnd.service.registration.TenantServiceImpl;
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
public class ChurchSeeder {
    private static final Logger logger = LoggerFactory.getLogger(ChurchSeeder.class);  // Use SLF4J logger

    private final ChurchRepository churchRepository;
    private final TenantServiceImpl tenantService;
    private final TenantRepository tenantRepository;

    public List<ChurchEntity> seedChurches(List<TenantEntity> tenants) {
        if (churchRepository.count() == 0) {
            Faker faker = new Faker();
            List<ChurchEntity> churches = new ArrayList<>();

            if (tenants.isEmpty()) {
                tenants = tenantService.getTenants();
            }




            for (TenantEntity tenant: tenants) {

                String churchCity = faker.address().city();
                String[] baseNames = { "St. Mary", "St. Michael", "St. Gabriel", "St. Antony", "St. John", "St. Paul" };
                // Pick one name randomly
                String randomBaseName = faker.options().option(baseNames);
                String churchName = randomBaseName + " " + churchCity;

                ChurchEntity church = ChurchEntity.builder()
                        .tenant(tenant)
                        .churchName(churchName)
                        .churchNumber("A" + faker.number().numberBetween(10000, 99999))
                        .email(faker.internet().emailAddress())
                        .diocese(faker.address().state())
                        .address(Address.builder()
                                .country(faker.country().name())
                                .province(faker.address().state())
                                .city(faker.address().city())
                                .street(faker.address().streetAddress())
                                .zipcode(faker.address().zipCode())
                                .build())
                        .build();

                ChurchEntity savedChurch = churchRepository.save(church);
                churches.add(savedChurch);
                tenant.assignChurch(savedChurch);
                tenantRepository.save(tenant);
            }

            logger.info("Seeded {} fake churches", churches.size());
            return churches;
        }

        return Collections.emptyList();
    }

}
