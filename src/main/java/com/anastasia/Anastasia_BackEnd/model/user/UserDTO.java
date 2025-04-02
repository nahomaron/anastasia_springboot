package com.anastasia.Anastasia_BackEnd.model.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserDTO {

    @NotBlank(message = "Full Name is required")
    @Size(min = 3, max = 30, message = "Full must be between 3 and 30 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_\\s]+$", message = "Username can only contain letters, numbers, white space, and underscores")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid Email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
            message = "Password must contain an uppercase letter, a lowercase letter, a number, and a special character")
    private String password;

    @NotBlank(message = "Confirm Password is required")
    private String confirmPassword;

    public boolean isPasswordMatch() {
        return this.password.equals(this.confirmPassword);
    }
}
