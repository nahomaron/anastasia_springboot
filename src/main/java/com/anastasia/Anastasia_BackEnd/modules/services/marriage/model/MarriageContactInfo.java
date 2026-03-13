package com.anastasia.Anastasia_BackEnd.modules.services.marriage.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Embeddable
public class MarriageContactInfo {

    @Column(name = "phone", length = 64)
    private String phone;

    @Column(name = "alternate_phone", length = 64)
    private String alternatePhone;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "address_line", length = 512)
    private String addressLine;

    @Column(name = "current_country", length = 128)
    private String currentCountry;

    @Column(name = "current_city", length = 128)
    private String currentCity;
}
