package com.anastasia.Anastasia_BackEnd.model.DTO;

import com.anastasia.Anastasia_BackEnd.model.entity.auth.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantDetailsDTO {

    private boolean activeTenant;

    private String phoneNumber;

    private String zipcode;

}
