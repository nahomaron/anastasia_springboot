package com.anastasia.Anastasia_BackEnd.modules.users.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TenantInviteRequest {
    @NotBlank(message = "validation.auth.email.required")
    @Email(message = "validation.auth.email.invalid")
    private String email;
}
