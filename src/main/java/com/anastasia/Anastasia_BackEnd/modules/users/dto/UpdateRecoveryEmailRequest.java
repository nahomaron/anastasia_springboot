package com.anastasia.Anastasia_BackEnd.modules.users.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateRecoveryEmailRequest {

    @NotBlank(message = "validation.user.recoveryEmail.required")
    @Email(message = "validation.user.recoveryEmail.invalid")
    private String recoveryEmail;
}
