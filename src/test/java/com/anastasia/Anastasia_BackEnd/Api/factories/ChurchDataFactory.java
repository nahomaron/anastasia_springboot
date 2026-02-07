package com.anastasia.Anastasia_BackEnd.Api.factories;

import com.anastasia.Anastasia_BackEnd.modules.registration.common.Address;
import com.anastasia.Anastasia_BackEnd.Api.utils.DataGenerator;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchDTO;

import java.util.concurrent.ThreadLocalRandom;

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
                .churchNameTigrinya("ቤተ ክርስቲያን " + DataGenerator.randomName().split(" ")[0])
                .diocese("North America Diocese")
                .phone("+1-555-" + ThreadLocalRandom.current().nextInt(1000, 10000))
                .denomination("Orthodox")
                .description("A welcoming parish community focused on outreach.")
                .usesOurServices(true)
                .address(Address.builder()
                        .addressLine1("456 Unity Rd")
                        .addressLine2("Suite 5")
                        .city("Fairfax")
                        .stateProvince("VA")
                        .country("USA")
                        .postalCode("22030")
                        .build())
                .email("admin+" + System.currentTimeMillis() + "@parish.org")
                .gpsLocation("38.8462,-77.3064")
                .instagram("https://instagram.com/parish")
                .youtube("https://youtube.com/@parish")
                .facebook("https://facebook.com/parish")
                .build();
    }

    public static ChurchDTO missingName() {
        ChurchDTO dto = newValidChurch();
        dto.setChurchName(null);
        return dto;
    }
}
