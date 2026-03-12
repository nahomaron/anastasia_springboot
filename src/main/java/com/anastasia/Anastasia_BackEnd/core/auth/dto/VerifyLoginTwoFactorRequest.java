package com.anastasia.Anastasia_BackEnd.core.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyLoginTwoFactorRequest {

    @NotBlank(message = "validation.auth.twoFactor.challengeToken.required")
    private String challengeToken;

    @NotBlank(message = "validation.auth.verificationCode.required")
    private String code;
}
