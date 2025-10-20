package com.anastasia.Anastasia_BackEnd.api.factories;

import com.anastasia.Anastasia_BackEnd.api.utils.DataGenerator;
import com.anastasia.Anastasia_BackEnd.model.common.Address;
import com.anastasia.Anastasia_BackEnd.model.priest.PriestDTO;

import java.util.Set;
import java.util.UUID;

/**
 * Factory for generating priest registration payloads.
 */
public final class PriestDataFactory {

    private static final String SECURE_PASSWORD = "Password@123";

    private PriestDataFactory() {
    }

    public static PriestDTO newValidPriest(String churchNumber, UUID tenantId) {
        return PriestDTO.builder()
                .churchNumber(churchNumber)
                .tenantId(tenantId)
                .profilePicture("https://cdn.example.com/priests/profile.png")
                .prefixes("Abba")
                .firstName(DataGenerator.randomName().split(" ")[0])
                .fatherName(DataGenerator.randomName().split(" ")[0])
                .grandFatherName(DataGenerator.randomName().split(" ")[0])
                .phoneNumber("+1202555" + (int) (Math.random() * 9000 + 1000))
                .personalEmail("priest+" + System.currentTimeMillis() + "@faith.org")
                .churchEmail("office+" + System.currentTimeMillis() + "@faith.org")
                .priesthoodCardId("CARD-" + System.currentTimeMillis())
                .priesthoodCardScan("https://cdn.example.com/priests/card.pdf")
                .birthdate("1978-04-12")
                .languages(Set.of("Tigrinya", "English"))
                .levelOfEducation("Theology Masters")
                .address(Address.builder()
                        .street("100 Blessing Rd")
                        .city("Dallas")
                        .province("TX")
                        .country("USA")
                        .zipcode("75001")
                        .build())
                .password(SECURE_PASSWORD)
                .confirmPassword(SECURE_PASSWORD)
                .build();
    }

    public static PriestDTO mismatchedPassword(String churchNumber, UUID tenantId) {
        PriestDTO dto = newValidPriest(churchNumber, tenantId);
        dto.setConfirmPassword("Mismatch123!");
        return dto;
    }
}
