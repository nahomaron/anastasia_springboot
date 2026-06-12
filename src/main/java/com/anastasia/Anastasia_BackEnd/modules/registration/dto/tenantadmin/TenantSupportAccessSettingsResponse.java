package com.anastasia.Anastasia_BackEnd.modules.registration.dto.tenantadmin;

import com.anastasia.Anastasia_BackEnd.modules.platform.admin.dto.SupportAccessSessionResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantSupportAccessSettingsResponse {
    private boolean enabled;
    private List<SupportAccessSessionResponse> recentHistory;
}
