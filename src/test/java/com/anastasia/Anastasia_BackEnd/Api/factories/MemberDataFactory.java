package com.anastasia.Anastasia_BackEnd.Api.factories;

import com.anastasia.Anastasia_BackEnd.Api.utils.DataGenerator;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.avatar.AvatarDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.common.Address;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.MemberDTO;

import java.time.LocalDate;

public final class MemberDataFactory {

    private MemberDataFactory() {
    }

    public static MemberDTO newValidMember() {
        return MemberDTO.builder()
                .churchNumber(generateChurchNumber())
                .avatar(new AvatarDTO("https://example.com/avatar.jpg", "200MB"))
                .deacon(false)
                .title("Mr.")
                .firstName(DataGenerator.randomName().split(" ")[0])
                .fatherName(DataGenerator.randomName().split(" ")[0])
                .grandFatherName(DataGenerator.randomName().split(" ")[0])
                .motherName(DataGenerator.randomName().split(" ")[0])
                .mothersFather(DataGenerator.randomName().split(" ")[0])
                .firstNameT("ትምህርቲ")
                .fatherNameT("ኣቦ")
                .grandFatherNameT("ኣቦኣቦ")
                .motherFullNameT("ኣደ ሚካኤል")
                .gender("Male")
                .birthday(LocalDate.of(1995, 5, 20))
                .nationality("Eritrean")
                .placeOfBirth("Asmara")
                .email(DataGenerator.randomEmail())
                .phone("+1408555" + (1000 + (int) (Math.random() * 8999)))
                .whatsApp("+1408555" + (1000 + (int) (Math.random() * 8999)))
                .emergencyContactNumber("+1408777" + (1000 + (int) (Math.random() * 8999)))
                .contactRelation("Brother")
                .eritreaContact("+291711111")
                .maritalStatus("Single")
                .numberOfChildren(0)
                .firstLanguage("Tigrinya")
                .secondLanguage("English")
                .profession("Software Engineer")
                .levelOfEducation("Masters")
                .fatherOfConfession("Abba Yohannes")
                .address(new Address("123 Church St", "Ithaca", "NY", "USA", "14850"))
                .build();
    }

    public static MemberDTO missingRequiredField() {
        MemberDTO dto = newValidMember();
        dto.setEmail(null);
        return dto;
    }

    public static MemberDTO invalidPhoneMember() {
        MemberDTO dto = newValidMember();
        dto.setPhone("12345");
        return dto;
    }

    public static MemberDTO femaleMember() {
        MemberDTO dto = newValidMember();
        dto.setGender("Female");
        dto.setTitle("Ms.");
        return dto;
    }

    private static String generateChurchNumber() {
        char prefix = (char) ('A' + (int) (Math.random() * 26));
        int number = 10000 + (int) (Math.random() * 89999);
        return prefix + String.valueOf(number);
    }
}
