package com.anastasia.Anastasia_BackEnd.modules.users.dto;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.avatar.AvatarDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class UpdateUserProfileRequest {

    @Size(max = 255, message = "Full name is too long")
    private String fullName;

    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @Size(max = 32, message = "Gender is too long")
    private String gender;

    @Size(max = 255, message = "Location is too long")
    private String location;

    @Pattern(regexp = "^(?:\\+|00)\\d{7,15}$", message = "Invalid international phone number format")
    private String phoneNumber;

    @Valid
    private AvatarDTO profileAvatar;

    @Size(max = 1024, message = "Profile image URL is too long")
    private String profileImageUrl;
}
