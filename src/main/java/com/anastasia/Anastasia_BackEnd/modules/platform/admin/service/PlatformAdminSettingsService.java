package com.anastasia.Anastasia_BackEnd.modules.platform.admin.service;

import com.anastasia.Anastasia_BackEnd.modules.platform.admin.dto.PlatformAdminSettingsResponse;
import com.anastasia.Anastasia_BackEnd.modules.platform.admin.dto.PlatformAdminSettingsUpdateRequest;
import com.anastasia.Anastasia_BackEnd.modules.platform.admin.dto.PlatformAnnouncementChannel;
import com.anastasia.Anastasia_BackEnd.modules.platform.admin.model.PlatformAdminSettingsEntity;
import com.anastasia.Anastasia_BackEnd.modules.platform.admin.repository.PlatformAdminSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlatformAdminSettingsService {

    private final PlatformAdminSettingsRepository repository;

    private static final Set<PlatformAnnouncementChannel> DEFAULT_CHANNELS = EnumSet.of(
            PlatformAnnouncementChannel.EMAIL,
            PlatformAnnouncementChannel.IN_APP
    );

    public PlatformAdminSettingsResponse getSettings() {
        return repository.findAll().stream().findFirst()
                .map(this::toResponse)
                .orElseGet(() -> toResponse(createDefaultSettings()));
    }

    @Transactional
    public PlatformAdminSettingsResponse updateSettings(PlatformAdminSettingsUpdateRequest request) {
        PlatformAdminSettingsEntity entity = repository.findAll().stream().findFirst().orElseGet(this::createDefaultSettings);
        entity.setMaintenanceMode(request.isMaintenanceMode());
        entity.setAutoRenewalInterval(request.getAutoRenewalInterval());
        entity.setSupportHours(request.getSupportHours());
        entity.setCustomerSuccessEmail(request.getCustomerSuccessEmail());
        entity.getAnnouncementChannels().clear();
        entity.getAnnouncementChannels().addAll(request.getAnnouncementChannels());
        entity.setEnableAutoAssignPriests(request.isEnableAutoAssignPriests());
        entity.setEnableManualPlanOverrides(request.isEnableManualPlanOverrides());
        repository.save(entity);
        return toResponse(entity);
    }

    private PlatformAdminSettingsEntity createDefaultSettings() {
        PlatformAdminSettingsEntity entity = new PlatformAdminSettingsEntity();
        entity.setId(UUID.randomUUID());
        entity.setMaintenanceMode(false);
        entity.setAutoRenewalInterval("MONTHLY");
        entity.setSupportHours("Mon–Fri 08:00–18:00 ET");
        entity.setCustomerSuccessEmail("care@anastasia.app");
        entity.getAnnouncementChannels().clear();
        entity.getAnnouncementChannels().addAll(DEFAULT_CHANNELS);
        entity.setEnableAutoAssignPriests(true);
        entity.setEnableManualPlanOverrides(true);
        return repository.save(entity);
    }

    private PlatformAdminSettingsResponse toResponse(PlatformAdminSettingsEntity entity) {
        return PlatformAdminSettingsResponse.builder()
                .maintenanceMode(entity.isMaintenanceMode())
                .autoRenewalInterval(entity.getAutoRenewalInterval())
                .supportHours(entity.getSupportHours())
                .customerSuccessEmail(entity.getCustomerSuccessEmail())
                .announcementChannels(entity.getAnnouncementChannels().stream().toList())
                .enableAutoAssignPriests(entity.isEnableAutoAssignPriests())
                .enableManualPlanOverrides(entity.isEnableManualPlanOverrides())
                .build();
    }
}
