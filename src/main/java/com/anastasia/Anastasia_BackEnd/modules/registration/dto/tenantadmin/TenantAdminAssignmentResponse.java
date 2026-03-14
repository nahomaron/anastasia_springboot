package com.anastasia.Anastasia_BackEnd.modules.registration.dto.tenantadmin;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.MembershipStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantAdminAssignmentResponse {
    private UUID id;
    private UUID tenantId;
    private UUID userId;
    private TenantRole role;
    private MembershipStatus status;
    private boolean billingContact;
    private UUID createdByUserId;
    private UUID updatedByUserId;
    private Instant createdAt;
    private Instant updatedAt;
}
