package com.anastasia.Anastasia_BackEnd.modules.registration.dto.tenantadmin;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCurrentTenantFeatureRequest {
    @NotNull
    private Boolean enabled;
}
