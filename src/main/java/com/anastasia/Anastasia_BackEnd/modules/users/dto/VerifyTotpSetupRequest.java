package com.anastasia.Anastasia_BackEnd.modules.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyTotpSetupRequest {

    @NotBlank(message = "validation.auth.verificationCode.required")
    @Pattern(regexp = "^\\d{6}$", message = "validation.auth.verificationCode.totp")
    private String code;
}
