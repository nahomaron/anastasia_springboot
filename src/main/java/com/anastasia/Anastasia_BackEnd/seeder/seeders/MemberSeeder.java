package com.anastasia.Anastasia_BackEnd.seeder.seeders;


import com.anastasia.Anastasia_BackEnd.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.model.common.Address;
import com.anastasia.Anastasia_BackEnd.model.member.MemberEntity;
import com.anastasia.Anastasia_BackEnd.model.user.UserEntity;
import com.anastasia.Anastasia_BackEnd.model.user.UserType;
import com.anastasia.Anastasia_BackEnd.repository.auth.UserRepository;
import com.anastasia.Anastasia_BackEnd.repository.registration.MemberRepository;
import com.anastasia.Anastasia_BackEnd.service.registration.ChurchServiceImpl;
import lombok.RequiredArgsConstructor;
import net.datafaker.Faker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.time.ZoneId;


@Component
@RequiredArgsConstructor
public class MemberSeeder {
    private static final Logger logger = LoggerFactory.getLogger(MemberSeeder.class);  // ✅ Use SLF4J logger

    private final MemberRepository memberRepository;
    private final ChurchServiceImpl churchService;
    private final ChurchSeeder churchSeeder;
    private final UserRepository userRepository;

    public void seedMembers(List<ChurchEntity> churches) {
        if (memberRepository.count() == 0) {
            Faker faker = new Faker();
            List<MemberEntity> members = new ArrayList<>();

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
                        .userType(UserType.MEMBER)
                        .build();

                UserEntity savedUser = userRepository.save(user);


                LocalDate birthday = faker.date().between(startDate, endDate).toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate();

                MemberEntity member = MemberEntity.builder()
                        .churchNumber(assignedChurch.getChurchNumber())
                        .church(assignedChurch)
                        .membershipNumber("M" + faker.number().numberBetween(10000,99999 ))
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
                        .eritreaContact("+2917" + faker.number().digits(7))
                        .maritalStatus(faker.options().option("Single", "Married", "Divorced", "Widowed"))
                        .numberOfChildren(faker.number().numberBetween(0, 4))
                        .firstLanguage(faker.nation().language())
                        .address(Address.builder()
                                .country(faker.country().name())
                                .province(faker.address().state())
                                .city(faker.address().city())
                                .street(faker.address().streetAddress())
                                .zipcode(faker.address().zipCode())
                                .build())
                        .secondLanguage(faker.options().option("English", "Arabic", "Amharic", null))
                        .profession(faker.company().profession())
                        .levelOfEducation(faker.options().option("High School", "Diploma", "Bachelor's", "Master's"))
                        .fatherOfConfession("Abune " + faker.name().firstName())
                        .user(savedUser) // 🔁 Link to the user
                        .build();

                MemberEntity savedMember = memberRepository.save(member);
                members.add(savedMember);

                user.assignMembership(savedMember);
                user.assignTenant(assignedChurch.getTenant());
                user.setUserType(UserType.MEMBER);
                userRepository.save(user);
            }

//            memberRepository.saveAll(members);
            logger.info("Seeded {} fake members", members.size());
        }
    }


}
