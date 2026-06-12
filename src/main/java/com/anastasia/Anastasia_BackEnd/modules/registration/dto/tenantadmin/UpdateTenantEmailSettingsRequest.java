package com.anastasia.Anastasia_BackEnd.modules.registration.dto.tenantadmin;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateTenantEmailSettingsRequest {
    private Boolean quotaEnforced;
    private Boolean sendingSuspended;

    @Min(0)
    private Integer monthlyQuota;

    @Size(max = 512)
    private String suspensionReason;
}
