package com.anastasia.Anastasia_BackEnd.modules.platform.admin.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PlatformAdminSettingsResponse {
    private boolean maintenanceMode;
    private String autoRenewalInterval;
    private String supportHours;
    private String customerSuccessEmail;
    private List<PlatformAnnouncementChannel> announcementChannels;
    private boolean enableAutoAssignPriests;
    private boolean enableManualPlanOverrides;
}
