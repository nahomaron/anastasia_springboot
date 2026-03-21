package com.anastasia.Anastasia_BackEnd.modules.registration.model.priest;

import com.anastasia.Anastasia_BackEnd.modules.registration.common.Address;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.imageasset.ImageAssetDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
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

    private ImageAssetDTO avatar;

    private PriestStatus status;

    private String prefixes; //(additional title)

    @NotBlank(message = "{validation.priest.firstName.required}")
    private String firstName;

    @NotBlank(message = "{validation.priest.fatherName.required}")
    private String fatherName;

    @NotBlank(message = "{validation.priest.grandFatherName.required}")
    private String grandFatherName;

    @NotBlank(message = "{validation.priest.phone.required}")
    @Pattern(regexp = "^(?:\\+|00)\\d{1,3}\\d{6,12}$", message = "{validation.priest.phone.invalid}")
    private String phoneNumber;

    @NotBlank(message = "{validation.priest.personalEmail.required}")
    @Email(message = "{validation.priest.email.invalid}")
    private String personalEmail;

    @Email(message = "{validation.priest.email.invalid}")
    private String churchEmail;

    private String priesthoodCardId; // (if any)
    private String priesthoodCardScan;

    @NotBlank(message = "{validation.priest.birthdate.required}")
    private String birthdate;

    @Builder.Default
    private Set<String> languages = new HashSet<>();
    private String levelOfEducation;

    @Valid
    private Address address;


    @NotBlank(message = "{validation.priest.password.required}")
    @Size(min = 8, message = "{validation.priest.password.min}")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
            message = "{validation.priest.password.pattern}")
    private String password;

    @NotBlank(message = "{validation.priest.confirmPassword.required}")
    private String confirmPassword;

    @AssertTrue(message = "{onboarding.password.mismatch}")
    public boolean isPasswordMatch() {
        return this.password != null && this.password.equals(this.confirmPassword);
    }

}
