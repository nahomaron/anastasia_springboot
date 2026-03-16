package com.anastasia.Anastasia_BackEnd.seeder.seeders;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.common.Address;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChurchRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.TenantServiceImpl;
import lombok.RequiredArgsConstructor;
import net.datafaker.Faker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
@Profile("!test") // disable in test profile
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

            String[] prefixes = {"St.", "Saint", "Holy", "Our Lady of", "Chapel of"};
            String[] patronNames = {"Mary", "Michael", "Gabriel", "Antony", "John", "Paul", "George", "Joseph"};
            String[] denominations = {"Orthodox", "Catholic", "Protestant", "Evangelical", "Non-denominational"};

            for (TenantEntity tenant: tenants) {

                String churchCity = faker.address().city();
                String prefix = faker.options().option(prefixes);
                String patronName = faker.options().option(patronNames);
                String baseName = patronName + " " + churchCity;
                String churchName = baseName;

                boolean usesOurServices = faker.bool().bool();
                String latitude = faker.address().latitude();
                String longitude = faker.address().longitude();

                ChurchEntity church = ChurchEntity.builder()
                        .tenant(tenant)
                        .prefix(prefix)
                        .prefixLocal(prefix + "-T")
                        .churchName(churchName)
                        .churchNameLocal(churchName + "-T")
                        .churchNumber("A" + faker.number().numberBetween(10000, 99999))
                        .email(faker.internet().emailAddress())
                        .phone(faker.phoneNumber().phoneNumber())
                        .neighborhood(faker.address().streetName())
                        .neighborhoodLocal(faker.address().streetName() + "-T")
                        .diocese(faker.address().state())
                        .denomination(faker.options().option(denominations))
                        .description(faker.lorem().paragraph())
                        .usesOurServices(usesOurServices)
                        .gpsLocation(latitude + "," + longitude)
                        .instagram("https://instagram.com/" + faker.internet().username().replaceAll("[^A-Za-z0-9_.]", ""))
                        .youtube("https://youtube.com/" + faker.internet().username().replaceAll("[^A-Za-z0-9]", ""))
                        .facebook("https://facebook.com/" + faker.internet().username().replaceAll("[^A-Za-z0-9.]", ""))
                        .address(Address.builder()
                                .addressLine1(faker.address().streetAddress())
                                .addressLine2(faker.address().secondaryAddress())
                                .country(faker.country().name())
                                .city(churchCity)
                                .stateProvince(faker.address().state())
                                .postalCode(faker.address().zipCode())
                                .build())
                        .build();

                ChurchEntity savedChurch = churchRepository.save(church);
                churches.add(savedChurch);
                tenant.assignChurch(savedChurch);
                tenantRepository.save(tenant);
            }

//            logger.info("Seeded {} fake churches", churches.size());
            return churches;
        }

        return Collections.emptyList();
    }

}
