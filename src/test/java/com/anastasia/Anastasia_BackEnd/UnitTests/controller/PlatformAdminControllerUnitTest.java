package com.anastasia.Anastasia_BackEnd.UnitTests.controller;

import com.anastasia.Anastasia_BackEnd.UnitTests.support.LenientMockitoTest;
import com.anastasia.Anastasia_BackEnd.modules.platform.admin.controller.PlatformAdminController;
import com.anastasia.Anastasia_BackEnd.modules.platform.admin.dto.PlatformAdminSettingsResponse;
import com.anastasia.Anastasia_BackEnd.modules.platform.admin.dto.PlatformAdminSettingsUpdateRequest;
import com.anastasia.Anastasia_BackEnd.modules.platform.admin.dto.PlatformAnnouncementChannel;
import com.anastasia.Anastasia_BackEnd.modules.platform.admin.service.PlatformAdminActionService;
import com.anastasia.Anastasia_BackEnd.modules.platform.admin.service.PlatformAdminReportService;
import com.anastasia.Anastasia_BackEnd.modules.platform.admin.service.PlatformAdminSettingsService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@LenientMockitoTest
class PlatformAdminControllerUnitTest {

    @Mock private PlatformAdminReportService reportService;
    @Mock private PlatformAdminSettingsService settingsService;
    @Mock private PlatformAdminActionService actionService;

    @InjectMocks private PlatformAdminController platformAdminController;

    @Test
    void updateSettingsDelegatesRequestBodyToService() {
        PlatformAdminSettingsUpdateRequest request = PlatformAdminSettingsUpdateRequest.builder()
                .maintenanceMode(true)
                .autoRenewalInterval("MONTHLY")
                .supportHours("Mon-Fri 08:00-18:00 ET")
                .customerSuccessEmail("care@anastasia.app")
                .announcementChannels(List.of(PlatformAnnouncementChannel.EMAIL, PlatformAnnouncementChannel.IN_APP))
                .enableAutoAssignPriests(true)
                .enableManualPlanOverrides(false)
                .build();
        PlatformAdminSettingsResponse expected = PlatformAdminSettingsResponse.builder()
                .maintenanceMode(true)
                .autoRenewalInterval("MONTHLY")
                .supportHours("Mon-Fri 08:00-18:00 ET")
                .customerSuccessEmail("care@anastasia.app")
                .announcementChannels(List.of(PlatformAnnouncementChannel.EMAIL, PlatformAnnouncementChannel.IN_APP))
                .enableAutoAssignPriests(true)
                .enableManualPlanOverrides(false)
                .build();
        when(settingsService.updateSettings(request)).thenReturn(expected);

        var response = platformAdminController.updateSettings(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
        verify(settingsService).updateSettings(request);
    }
}
