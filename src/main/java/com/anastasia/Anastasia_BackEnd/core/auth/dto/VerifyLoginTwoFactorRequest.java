package com.anastasia.Anastasia_BackEnd.core.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyLoginTwoFactorRequest {

    @NotBlank(message = "Challenge token is required")
    private String challengeToken;

    @NotBlank(message = "Verification code is required")
    private String code;
}
