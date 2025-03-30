package com.anastasia.Anastasia_BackEnd.model.DTO.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangePasswordRequest {

    private String currentPassword;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[@#$%^&+=]).+$",
            message = "Password must contain an uppercase letter, a lowercase letter, a number, and a special character")
    private String newPassword;

    @NotBlank(message = "Confirm Password is required")
    private String confirmNewPassword;

    public boolean isPasswordMatch() {
        return this.newPassword.equals(this.confirmNewPassword);
    }
}
