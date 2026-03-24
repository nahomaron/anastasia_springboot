package com.anastasia.Anastasia_BackEnd.modules.registration.dto.tenantadmin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCurrentTenantSettingsRequest {
    private UpdateTenantAttendanceSettingsRequest attendance;
}
