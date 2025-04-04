package com.anastasia.Anastasia_BackEnd.model.common;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Embeddable
public class Address {

    @NotBlank(message = "Street is required")
    private String street;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "Province/State is required")
    private String province;

    @NotBlank(message = "Country is required")
    private String country;

    @Pattern(regexp = "^\\d{4,10}$", message = "Invalid postal code format")
    private String zipcode;
}
