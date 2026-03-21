package com.anastasia.Anastasia_BackEnd.modules.users.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class TenantUserAccessResponse {
    private UUID userId;
    private UUID tenantId;
    private boolean protectedAccount;
    private String protectedReason;
    private boolean canEdit;
    private List<TenantAssignedRoleResponse> assignedRoles;
    private List<String> directPermissions;
    private List<String> effectivePermissions;
}
