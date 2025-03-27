package com.anastasia.Anastasia_BackEnd.model.DTO;

import com.anastasia.Anastasia_BackEnd.model.entity.auth.UserEntity;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantDTO {

    private boolean isActiveTenant;

    private String phoneNumber;

    private String zipcode;

}
