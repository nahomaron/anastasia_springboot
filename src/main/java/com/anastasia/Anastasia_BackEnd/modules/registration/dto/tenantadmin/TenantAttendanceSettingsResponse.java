package com.anastasia.Anastasia_BackEnd.modules.registration.dto.tenantadmin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantAttendanceSettingsResponse {
    private boolean kioskModeEnabled;
    private boolean newcomerCaptureEnabled;
    private TenantAttendanceCaptureFieldsResponse captureFields;
}
