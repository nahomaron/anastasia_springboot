package com.anastasia.Anastasia_BackEnd.modules.platform.admin.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PlatformAdminInviteResponse {
    PlatformAdminUserResponse admin;
    boolean onboardingEmailSent;
    String message;
}
