package com.anastasia.Anastasia_BackEnd.modules.registration.model.church;

import com.anastasia.Anastasia_BackEnd.modules.registration.common.Address;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChurchDTO {

    private Long churchId;

    private String churchNumber;

    private String prefix;

    @NotBlank(message = "Church name is required ex. St. Mary")
    private String churchName;

    @NotBlank(message = "Diocese is required")
    private String diocese;

    @Valid
    private Address address;

    @NotBlank(message = "Church email is required")
    private String email;

    private String phone;

    private String denomination;

    private String description;

    private boolean usesOurServices;

    private String gpsLocation;

    private String instagram;
    private String youtube;
    private String facebook;

}
