package com.anastasia.Anastasia_BackEnd.model.church;

import com.anastasia.Anastasia_BackEnd.model.common.Address;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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


    private String prefix;

    @NotBlank(message = "Church name is required ex. St. Mary")
    private String churchName;

    @NotBlank(message = "Diocese is required")
    private String diocese;

    @Valid
    private Address address;

    @NotBlank(message = "Church email is required")
    private String email;


    private String gpsLocation;
    private String websiteUrl;
    private String youtubePage;
    private String facebookPage;

}
