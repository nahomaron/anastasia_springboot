package com.anastasia.Anastasia_BackEnd.core.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuthenticationRequest {

    @NotBlank(message = "validation.auth.email.required")
    @Email(message = "validation.auth.email.invalid")
    private String email;

    @NotBlank(message = "validation.auth.password.required")
    private String password;
}
