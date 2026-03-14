package com.anastasia.Anastasia_BackEnd.modules.registration.dto.tenantadmin;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantRole;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InviteTenantAdminRequest {
    @NotNull
    private UUID userId;
    private TenantRole role;
    private Boolean billingContact;
}
