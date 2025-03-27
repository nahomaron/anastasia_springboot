package com.anastasia.Anastasia_BackEnd.model.entity.embeded;

import com.anastasia.Anastasia_BackEnd.model.entity.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.model.entity.auth.Role;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class TenantDetails {

    private boolean activeTenant;

    private String phoneNumber;

    private String zipcode;

}
