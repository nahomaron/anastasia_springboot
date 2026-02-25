package com.anastasia.Anastasia_BackEnd.modules.users.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TenantMembershipActionRequest {
    @NotNull(message = "Action is required")
    private TenantMembershipAction action;
}
