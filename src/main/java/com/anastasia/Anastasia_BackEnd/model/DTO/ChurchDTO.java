package com.anastasia.Anastasia_BackEnd.model.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChurchDTO {

    private UUID tenantId;
    private String churchName;
    private String street;
    private String city;
    private String country;
    private String diocese;
    private String prefix;
    private String email;
    private String gpsLocation;
    private String websiteUrl;
    private String youtubePage;
    private String facebookPage;

}
