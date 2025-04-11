package com.anastasia.Anastasia_BackEnd.seeder.seeders;


import com.anastasia.Anastasia_BackEnd.model.child.ChildEntity;
import com.anastasia.Anastasia_BackEnd.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.model.common.Address;
import com.anastasia.Anastasia_BackEnd.model.user.UserEntity;
import com.anastasia.Anastasia_BackEnd.repository.auth.UserRepository;
import com.anastasia.Anastasia_BackEnd.repository.registration.ChildRepository;
import com.anastasia.Anastasia_BackEnd.service.registration.ChurchServiceImpl;
import lombok.RequiredArgsConstructor;
import net.datafaker.Faker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


@Component
@RequiredArgsConstructor
public class ChildSeeder {
    private static final Logger logger = LoggerFactory.getLogger(ChildSeeder.class);  // ✅ Use SLF4J logger

    private final ChildRepository childRepository;
    private final ChurchServiceImpl churchService;
    private final ChurchSeeder churchSeeder;
    private final UserRepository userRepository;

    public void seedChildren(List<ChurchEntity> churches) {
        if (childRepository.count() == 0) {
            Faker faker = new Faker();
            List<ChildEntity> children = new ArrayList<>();

            Date startDate = Date.from(LocalDate.now().minusYears(60).atStartOfDay(ZoneId.systemDefault()).toInstant());
            Date endDate = Date.from(LocalDate.now().minusYears(18).atStartOfDay(ZoneId.systemDefault()).toInstant());

            if (churches.isEmpty()) {
                churches = churchService.getChurches();
            }

            List<ChurchEntity> selectedChurches = churches.subList(0, Math.min(10, churches.size()));

            for (int i = 0; i <= 50; i++) {
                ChurchEntity assignedChurch = selectedChurches.get(i % selectedChurches.size());

                String password = faker.internet().password(8, 12, true, true, true) + "@1A";

                UserEntity user = UserEntity.builder()
                        .fullName(faker.name().fullName())
                        .email(faker.internet().emailAddress())
                        .password(password)
                        .build();

                UserEntity savedUser = userRepository.save(user);



                LocalDate birthday = faker.date().between(startDate, endDate).toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate();

                ChildEntity childMember = ChildEntity.builder()
                        .churchNumber(assignedChurch.getChurchNumber())
                        .status(faker.options().option("PENDING", "APPROVED", "REJECTED", "ACTIVE", "BLOCKED"))
                        .deacon(faker.bool().bool())
                        .title(faker.options().option("Mr.", "Ms.", "Mrs.", "Dr."))
                        .firstName(faker.name().firstName())
                        .fatherName(faker.name().lastName())
                        .grandFatherName(faker.name().lastName())
                        .motherName(faker.name().firstName())
                        .mothersFather(faker.name().lastName())
                        .firstNameT(faker.options().option("ሚካኤል", "በረኸት", "ሓረጉ", "ገብራይ", "ወልዳይ", "ተስፉ", "መሓሪት", "ሳራ", "ልዋም", "ሄርሞን"))
                        .fatherNameT(faker.options().option("ተስፋይ", "መንግስትኣብ", "ብርሃነ", "ርእሶም", "ጠዓመ", "በራኺ", "ዝኾነ", "ተኽለብርሃን", "ወልደማርያም"))
                        .grandFatherNameT("ገብረ")
                        .motherFullNameT("ሳባ ኣማኑኤል")
                        .gender(faker.options().option("Male", "Female"))
                        .birthday(birthday)
                        .nationality(faker.nation().nationality())
                        .placeOfBirth(faker.address().city())
                        .email(user.getEmail()) // link same email or generate a different one
                        .phone("+2917" + faker.number().digits(7))
                        .whatsApp("+2917" + faker.number().digits(7))
                        .emergencyContactNumber("+2917" + faker.number().digits(7))
                        .contactRelation(faker.options().option("Brother", "Sister", "Uncle", "Aunt", "Friend"))
                        .firstLanguage(faker.nation().language())
                        .address(Address.builder()
                                .country(faker.country().name())
                                .province(faker.address().state())
                                .city(faker.address().city())
                                .street(faker.address().streetAddress())
                                .build())
                        .secondLanguage(faker.options().option("English", "Arabic", "Amharic", null))
                        .levelOfEducation(faker.options().option("High School", "Diploma", "Bachelor's", "Master's"))
                        .fatherOfConfession("Abune " + faker.name().firstName())
                        .user(savedUser) // 🔁 Link to the user
                        .build();

                children.add(childMember);
            }

            childRepository.saveAll(children);
            logger.info("Seeded {} fake members", children.size());
        }
    }


}
