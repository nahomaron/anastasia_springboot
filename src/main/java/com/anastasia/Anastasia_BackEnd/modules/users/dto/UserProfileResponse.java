package com.anastasia.Anastasia_BackEnd.modules.users.dto;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.imageasset.ImageAssetDTO;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.util.UUID;

@Value
@Builder
public class UserProfileResponse {
    UUID userId;
    String fullName;
    String email;
    LocalDate dateOfBirth;
    String gender;
    String location;
    String phoneNumber;
    boolean phoneVerified;
    String recoveryEmail;
    boolean recoveryEmailVerified;
    ImageAssetDTO profileAvatar;
    String profileImageUrl;
    boolean twoFactorEnabled;
    boolean totpConfigured;
    long backupCodesRemaining;
}
