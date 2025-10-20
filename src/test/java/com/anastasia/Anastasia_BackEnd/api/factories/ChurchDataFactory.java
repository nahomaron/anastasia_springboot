package com.anastasia.Anastasia_BackEnd.api.factories;

import com.anastasia.Anastasia_BackEnd.api.utils.DataGenerator;
import com.anastasia.Anastasia_BackEnd.model.church.ChurchDTO;
import com.anastasia.Anastasia_BackEnd.model.common.Address;

/**
 * Factory for creating {@link ChurchDTO} payloads.
 */
public final class ChurchDataFactory {

    private ChurchDataFactory() {
    }

    public static ChurchDTO newValidChurch() {
        return ChurchDTO.builder()
                .prefix("St.")
                .churchName("St. " + DataGenerator.randomName().split(" ")[0])
                .diocese("North America Diocese")
                .address(Address.builder()
                        .street("456 Unity Rd")
                        .city("Fairfax")
                        .province("VA")
                        .country("USA")
                        .zipcode("22030")
                        .build())
                .email("admin+" + System.currentTimeMillis() + "@parish.org")
                .gpsLocation("38.8462,-77.3064")
                .websiteUrl("https://parish.example.com")
                .facebookPage("https://facebook.com/parish")
                .youtubePage("https://youtube.com/@parish")
                .build();
    }

    public static ChurchDTO missingName() {
        ChurchDTO dto = newValidChurch();
        dto.setChurchName(null);
        return dto;
    }
}
