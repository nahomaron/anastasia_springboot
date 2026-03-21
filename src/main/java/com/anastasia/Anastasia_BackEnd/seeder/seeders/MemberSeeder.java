package com.anastasia.Anastasia_BackEnd.seeder.seeders;


import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.common.Address;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.EducationLevel;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.MemberGender;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.MemberLifecycleStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.Adult_MemberEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.MaritalStatus;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserType;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.MemberRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.ChurchServiceImpl;
import lombok.RequiredArgsConstructor;
import net.datafaker.Faker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Profile("!test") // disable in test profile
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
            List<Adult_MemberEntity> members = new ArrayList<>();

            if (churches.isEmpty()) {
                churches = churchService.getChurches();
            }

            List<ChurchEntity> selectedChurches = churches.subList(0, Math.min(10, churches.size()));

            for (int i = 0; i <= 50; i++) {
                ChurchEntity assignedChurch = selectedChurches.get(i % selectedChurches.size());

                String password = SeederRandomUtils.generateSecurePassword(8, 12);

                UserEntity user = UserEntity.builder()
                        .fullName(faker.name().fullName())
                        .email(faker.internet().emailAddress())
                        .password(password)
                        .userType(UserType.MEMBER)
                        .build();

                UserEntity savedUser = userRepository.save(user);

                LocalDate birthday = SeederRandomUtils.randomBirthdate(18, 60);

                UUID tenantId = Optional.ofNullable(assignedChurch.getTenant())
                        .map(tenant -> tenant.getId())
                        .orElseThrow(() -> new IllegalStateException("Assigned church has no tenant"));

                Adult_MemberEntity member = Adult_MemberEntity.builder()
                        .tenantId(tenantId)
                        .churchNumber(assignedChurch.getChurchNumber())
                        .church(assignedChurch)
                        .membershipNumber("M" + faker.number().numberBetween(10000,99999 ))
                        .statusValue(MemberLifecycleStatus.from(faker.options().option("PENDING", "APPROVED", "ACTIVE", "NON_ACTIVE", "DECEASED")))
                        .deacon(faker.bool().bool())
                        .title(faker.options().option("Mr.", "Ms.", "Mrs.", "Dr."))
                        .firstName(faker.name().firstName())
                        .fatherName(faker.name().lastName())
                        .grandFatherName(faker.name().lastName())
                        .motherName(faker.name().firstName())
                        .mothersFather(faker.name().lastName())
                        .firstNameLocal(faker.options().option("ሚካኤል", "በረኸት", "ሓረጉ", "ገብራይ", "ወልዳይ", "ተስፉ", "መሓሪት", "ሳራ", "ልዋም", "ሄርሞን"))
                        .fatherNameLocal(faker.options().option("ተስፋይ", "መንግስትኣብ", "ብርሃነ", "ርእሶም", "ጠዓመ", "በራኺ", "ዝኾነ", "ተኽለብርሃን", "ወልደማርያም"))
                        .grandFatherNameLocal("ገብረ")
                        .motherFullNameLocal("ሳባ ኣማኑኤል")
                        .genderValue(MemberGender.from(faker.options().option("Male", "Female")))
                        .birthday(birthday)
                        .nationality(faker.nation().nationality())
                        .placeOfBirth(faker.address().city())
                        .email(user.getEmail()) // link same email or generate a different one
                        .phone("+2917" + faker.number().digits(7))
                        .whatsApp("+2917" + faker.number().digits(7))
                        .emergencyContactNumber("+2917" + faker.number().digits(7))
                        .contactRelation(faker.options().option("Brother", "Sister", "Uncle", "Aunt", "Friend"))
                        .eritreaContact("+2917" + faker.number().digits(7))
                        .maritalStatus(faker.options().option(MaritalStatus.MARRIED, MaritalStatus.SINGLE))
                        .numberOfChildren(faker.number().numberBetween(0, 4))
                        .firstLanguage(faker.nation().language())
                        .termsAccepted(true)
                        .termsVersion("seed-v1")
                        .termsAcceptedAt(Instant.now())
                        .address(Address.builder()
                                .addressLine1(faker.address().streetAddress())
                                .addressLine2(faker.address().secondaryAddress())
                                .country(faker.country().name())
                                .city(faker.address().city())
                                .stateProvince(faker.address().state())
                                .postalCode(faker.address().zipCode())
                                .build())
                        .secondLanguage(faker.options().option("English", "Arabic", "Amharic", null))
                        .profession(faker.company().profession())
                        .educationLevelValue(EducationLevel.from(faker.options().option("High School", "Diploma", "Bachelor's", "Master's")))
                        .fatherOfConfession("Abune " + faker.name().firstName())
                        .user(savedUser) // 🔁 Link to the user
                        .build();

                UUID auditUserId = savedUser.getUuid();
                member.setCreatedBy(auditUserId);
                member.setUpdatedBy(auditUserId);
                Instant now = Instant.now();
                member.setCreatedAt(now);
                member.setUpdatedAt(now);

                Adult_MemberEntity savedMember = memberRepository.save(member);
                members.add(savedMember);

                user.assignMembership(savedMember);
                user.assignTenant(assignedChurch.getTenant());
                user.setUserType(UserType.MEMBER);
                userRepository.save(user);
            }

//            memberRepository.saveAll(members);
//            logger.info("Seeded {} fake members", members.size());
        }
    }


}
