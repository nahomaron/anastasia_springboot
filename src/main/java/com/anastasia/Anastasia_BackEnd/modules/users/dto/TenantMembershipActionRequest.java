package com.anastasia.Anastasia_BackEnd.modules.users.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TenantMembershipActionRequest {
    @NotNull(message = "validation.user.membership.action.required")
    private TenantMembershipAction action;
}
