package com.anastasia.Anastasia_BackEnd.modules.users.dto;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.imageasset.ImageAssetDTO;
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

    @Size(max = 255, message = "validation.user.profile.fullName.max")
    private String fullName;

    @Past(message = "validation.user.profile.dateOfBirth.past")
    private LocalDate dateOfBirth;

    @Size(max = 32, message = "validation.user.profile.gender.max")
    private String gender;

    @Size(max = 255, message = "validation.user.profile.location.max")
    private String location;

    @Pattern(regexp = "^(?:\\+|00)\\d{7,15}$", message = "validation.user.profile.phone.invalid")
    private String phoneNumber;

    @Valid
    private ImageAssetDTO profileAvatar;

    @Size(max = 1024, message = "validation.user.profile.imageUrl.max")
    private String profileImageUrl;
}
