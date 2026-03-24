package com.anastasia.Anastasia_BackEnd.modules.registration.dto.tenantadmin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateTenantAttendanceSettingsRequest {
    private Boolean kioskModeEnabled;
    private Boolean newcomerCaptureEnabled;
    private UpdateTenantAttendanceCaptureFieldsRequest captureFields;
}
