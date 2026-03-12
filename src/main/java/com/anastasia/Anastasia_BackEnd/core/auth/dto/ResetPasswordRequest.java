package com.anastasia.Anastasia_BackEnd.core.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * DTO for handling password reset requests.
 * Contains the token received via email, and the new password along with its confirmation.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordRequest {

    @NotBlank(message = "validation.auth.reset.token.required")
    private String token;

    @NotBlank(message = "validation.auth.reset.newPassword.required")
    @Size(min = 8, message = "validation.auth.reset.newPassword.min")
    private String newPassword;

    @NotBlank(message = "validation.auth.reset.confirmPassword.required")
    private String confirmNewPassword;

    public boolean isPasswordMatch() {
        return newPassword != null && newPassword.equals(confirmNewPassword);
    }
}
