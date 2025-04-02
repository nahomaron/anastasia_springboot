package com.anastasia.Anastasia_BackEnd.model.priest;

import com.anastasia.Anastasia_BackEnd.model.common.Address;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriestDTO {

    /**
     * Linking priest to a tenant or a church:
     * - If `tenantId` is provided, the priest is an independent tenant.
     * - If `churchNumber` is provided, the priest is a member of a church.
     */
    private String churchNumber;

    private UUID tenantId;

    private String profilePicture;

    private String prefixes; //(additional title)

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Father's name is required")
    private String fatherName;

    @NotBlank(message = "Grandfather's name is required")
    private String grandFatherName;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^(?:\\+|00)\\d{1,3}\\d{6,12}$", message = "Invalid phone number format")
    private String phoneNumber;

    @NotBlank(message = "Personal email is required")
    @Email(message = "Invalid email format")
    private String personalEmail;

    @Email(message = "Invalid email format")
    private String churchEmail;

    private String priesthoodCardId; // (if any)
    private String priesthoodCardScan;

    @NotBlank(message = "Birthdate is required")
    private String birthdate;

    private Set<String> languages = new HashSet<>();
    private String levelOfEducation;

    @Valid
    private Address address;


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
