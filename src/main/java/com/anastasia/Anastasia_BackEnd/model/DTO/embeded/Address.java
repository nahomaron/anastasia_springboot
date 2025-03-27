package com.anastasia.Anastasia_BackEnd.model.DTO.embeded;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Address {
    private String addressCountry;
    private String currentAddress;
    private String province;
    private String city;
}
