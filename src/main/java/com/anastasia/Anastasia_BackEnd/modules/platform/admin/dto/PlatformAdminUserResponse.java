package com.anastasia.Anastasia_BackEnd.modules.platform.admin.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Value
@Builder
public class PlatformAdminUserResponse {
    UUID userId;
    String fullName;
    String email;
    String status;
    List<String> roles;
    boolean breakGlass;
    boolean verified;
    boolean twoFactorEnabled;
    boolean mustChangePassword;
    Instant lastLoginAt;
    Instant createdAt;
}
