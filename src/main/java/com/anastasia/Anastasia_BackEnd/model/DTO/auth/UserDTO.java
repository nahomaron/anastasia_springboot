package com.anastasia.Anastasia_BackEnd.model.DTO.auth;

import com.anastasia.Anastasia_BackEnd.model.DTO.TenantDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserDTO {

    private String fullName;
    private String email;
    private String password;
}
