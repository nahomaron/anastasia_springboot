package com.anastasia.Anastasia_BackEnd.modules.platform.admin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PlatformAdminSettingsUpdateRequest {
    private boolean maintenanceMode;
    @NotBlank
    private String autoRenewalInterval;
    private String supportHours;
    @Email
    private String customerSuccessEmail;
    @NotNull
    private List<PlatformAnnouncementChannel> announcementChannels;
    private boolean enableAutoAssignPriests;
    private boolean enableManualPlanOverrides;
}
