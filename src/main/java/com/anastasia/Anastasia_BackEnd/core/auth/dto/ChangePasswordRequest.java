package com.anastasia.Anastasia_BackEnd.core.auth.dto;

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

    @NotBlank(message = "validation.auth.change.currentPassword.required")
    private String currentPassword;

    @NotBlank(message = "validation.auth.change.newPassword.required")
    @Size(min = 8, message = "validation.auth.change.newPassword.min")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
            message = "validation.auth.change.newPassword.pattern")
    private String newPassword;

    @NotBlank(message = "validation.auth.change.confirmPassword.required")
    private String confirmNewPassword;

    public boolean isPasswordMatch() {
        return this.newPassword.equals(this.confirmNewPassword);
    }
}
