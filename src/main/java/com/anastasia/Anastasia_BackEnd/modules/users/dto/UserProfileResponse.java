package com.anastasia.Anastasia_BackEnd.modules.users.dto;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.avatar.AvatarDTO;
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
    AvatarDTO profileAvatar;
    String profileImageUrl;
    boolean twoFactorEnabled;
    boolean totpConfigured;
    long backupCodesRemaining;
}
