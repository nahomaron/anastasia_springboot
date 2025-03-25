package com.anastasia.Anastasia_BackEnd.model.DTO.auth;

import com.anastasia.Anastasia_BackEnd.model.entity.auth.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserDTO {

    private String firstName;
    private String lastName;
    private String email;
    private String password;
//    private String googleId;
//    private String facebookId;

}
