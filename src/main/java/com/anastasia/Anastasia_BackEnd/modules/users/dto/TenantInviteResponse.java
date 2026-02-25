package com.anastasia.Anastasia_BackEnd.modules.users.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TenantInviteResponse {
    private String email;
    private boolean existingUser;
    private String message;
}
