package com.anastasia.Anastasia_BackEnd.seeder.seeders;

import com.anastasia.Anastasia_BackEnd.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.model.common.Address;
import com.anastasia.Anastasia_BackEnd.model.priest.PriestEntity;
import com.anastasia.Anastasia_BackEnd.model.priest.PriestStatus;
import com.anastasia.Anastasia_BackEnd.model.user.UserEntity;
import com.anastasia.Anastasia_BackEnd.model.user.UserType;
import com.anastasia.Anastasia_BackEnd.repository.PriestRepository;
import com.anastasia.Anastasia_BackEnd.repository.auth.UserRepository;
import com.anastasia.Anastasia_BackEnd.service.registration.ChurchServiceImpl;
import com.anastasia.Anastasia_BackEnd.service.registration.TenantServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.*;

@Profile("!test") // disable in test profile
@Slf4j
@Component
@RequiredArgsConstructor
public class PriestSeeder {

    private final PriestRepository priestRepository;
    private final UserRepository userRepository;
    private final ChurchSeeder churchSeeder;
    private final ChurchServiceImpl churchService;
    private final TenantServiceImpl tenantService;

    public void seedPriests(List<ChurchEntity> churches) {

        if(priestRepository.count() == 0) {
            net.datafaker.Faker faker = new Faker();
            List<PriestEntity> priests = new ArrayList<>();
            Set<String> generatedPriestNumbers = new HashSet<>();
            Set<String> generatedPhoneNumbers = new HashSet<>();

            if (churches.isEmpty()) {
                churches = churchService.getChurches();
            }

            List<ChurchEntity> selectedChurches = churches.subList(0, Math.min(10, churches.size()));


            for (int i = 0; i < 30; i++) {
                ChurchEntity assignedChurch = selectedChurches.get(i % selectedChurches.size());


                Address address = Address.builder()
                        .city(faker.address().city())
                        .country(faker.address().country())
                        .street(faker.address().streetAddress())
                        .province(faker.address().state())
                        .zipcode(faker.address().zipCode())
                        .build();

                String password = faker.internet().password(8, 12, true, true, true) + "@1A";

                UserEntity user = UserEntity.builder()
                        .fullName(faker.name().fullName())
                        .email(faker.internet().emailAddress())
                        .password(password)
                        .userType(UserType.PRIEST)
                        .build();

                UserEntity savedUser = userRepository.save(user);

                Set<String> languages = new HashSet<>();
                languages.add(faker.nation().language());
                languages.add(faker.options().option("Amharic", "Tigrinya", "Oromo", "English"));

                String priestNumber;
                do {
                    priestNumber = "P" + faker.number().numberBetween(10000, 99999);
                } while (!generatedPriestNumbers.add(priestNumber));

                String phoneNumber;
                do {
                    phoneNumber = "+2519" + faker.number().digits(8);
                } while (!generatedPhoneNumbers.add(phoneNumber));

                PriestEntity priest = PriestEntity.builder()
                        .priestNumber(priestNumber)
                        .user(savedUser)
                        .church(assignedChurch)
                        .churchNumber(assignedChurch.getChurchNumber())
                        .tenant(null)
                        .status(faker.options().option(PriestStatus.ACTIVE, PriestStatus.APPROVED, PriestStatus.PENDING, PriestStatus.INACTIVE))
                        .profilePicture(faker.internet().url())
                        .prefixes(faker.name().prefix())
                        .firstName(faker.name().firstName())
                        .fatherName(faker.name().lastName())
                        .grandFatherName(faker.name().lastName())
                        .phoneNumber(phoneNumber)
                        .churchEmail(faker.internet().emailAddress("church"))
                        .priesthoodCardId(faker.idNumber().valid())
                        .priesthoodCardScan("scan_" + faker.file().fileName())
                        .birthdate(faker.date().birthday(30, 70).toString())
                        .languages(languages)
                        .levelOfEducation(faker.educator().course())
                        .address(address)
                        .isActive(faker.bool().bool())
                        .build();

                priests.add(priest);

            }


            List<PriestEntity> savedPriests = priestRepository.saveAll(priests);
//            log.info("Seeded {}", savedPriests.size());
//            return savedPriests;
        }

//        return Collections.emptyList();
    }
}
