package com.anastasia.Anastasia_BackEnd.core.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PlatformAdminRegistrationRequest {

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String password;

    private String fullName;
}
