package com.anastasia.Anastasia_BackEnd.Api.factories;

import com.anastasia.Anastasia_BackEnd.Api.utils.DataGenerator;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child.Child_MemberDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.common.Address;

import java.time.LocalDate;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Factory for provisioning child registration payloads.
 */
public final class ChildDataFactory {

    private ChildDataFactory() {
    }

    public static Child_MemberDTO newValidChild() {
        return Child_MemberDTO.builder()
                .churchNumber(generateChurchNumber())
                .deacon(false)
                .title("Child")
                .firstName(randomFirstName())
                .fatherName(randomLastName())
                .grandFatherName(randomLastName())
                .motherName(randomFirstName())
                .mothersFather(randomLastName())
                .firstNameT("ኣብርሃ")
                .fatherNameT("ገብረ")
                .grandFatherNameT("ኣቡን")
                .motherFullNameT("ኣምላኽ ማርያም")
                .gender("Male")
                .birthday(randomBirthday())
                .nationality("Eritrean")
                .placeOfBirth("Asmara")
                .email(DataGenerator.randomEmail())
                .phone("+1202555" + randomDigits(4))
                .whatsApp("+1202666" + randomDigits(4))
                .emergencyContactNumber("+1202777" + randomDigits(4))
                .contactRelation("Parent")
                .firstLanguage("Tigrinya")
                .secondLanguage("English")
                .levelOfEducation("Grade 3")
                .fatherOfConfession("Abba Tekle")
                .address(Address.builder()
                        .street("123 Joy Ave")
                        .city("Silver Spring")
                        .province("MD")
                        .country("USA")
                        .zipcode("20910")
                        .build())
                .build();
    }

    public static Child_MemberDTO missingRequiredField() {
        Child_MemberDTO dto = newValidChild();
        dto.setFirstName(null);
        return dto;
    }

    public static Child_MemberDTO invalidPhoneChild() {
        Child_MemberDTO dto = newValidChild();
        dto.setPhone("12345");
        return dto;
    }

    private static String generateChurchNumber() {
        char first = (char) ('A' + ThreadLocalRandom.current().nextInt(26));
        char second = (char) ('A' + ThreadLocalRandom.current().nextInt(26));
        int number = 10000 + ThreadLocalRandom.current().nextInt(90000);
        return "" + first + second + number;
    }

    private static LocalDate randomBirthday() {
        return LocalDate.now().minusYears(8 + ThreadLocalRandom.current().nextInt(6))
                .withMonth(1 + ThreadLocalRandom.current().nextInt(12))
                .withDayOfMonth(1 + ThreadLocalRandom.current().nextInt(20));
    }

    private static String randomFirstName() {
        return DataGenerator.randomName().split(" ")[0];
    }

    private static String randomLastName() {
        return DataGenerator.randomName().split(" ")[0];
    }

    private static String randomDigits(int count) {
        return String.valueOf(ThreadLocalRandom.current().nextInt((int) Math.pow(10, count - 1),
                (int) Math.pow(10, count)));
    }
}
