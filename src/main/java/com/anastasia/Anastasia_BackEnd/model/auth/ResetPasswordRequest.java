package com.anastasia.Anastasia_BackEnd.model.auth;

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

    @NotBlank(message = "Reset token is required.")
    private String token;

    @NotBlank(message = "New password is required.")
    @Size(min = 8, message = "New password must be at least 8 characters long.")
    private String newPassword;

    @NotBlank(message = "Password confirmation is required.")
    private String confirmNewPassword;

    public boolean isPasswordMatch() {
        return newPassword != null && newPassword.equals(confirmNewPassword);
    }
}
