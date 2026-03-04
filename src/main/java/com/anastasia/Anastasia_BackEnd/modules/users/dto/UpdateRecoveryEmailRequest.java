package com.anastasia.Anastasia_BackEnd.modules.users.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateRecoveryEmailRequest {

    @NotBlank(message = "Recovery email is required")
    @Email(message = "Recovery email must be a valid email")
    private String recoveryEmail;
}
