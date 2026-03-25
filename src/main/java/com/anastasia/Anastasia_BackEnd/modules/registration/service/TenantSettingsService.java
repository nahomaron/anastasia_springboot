package com.anastasia.Anastasia_BackEnd.modules.registration.service;

import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.common.i18n.LocalizedMessageService;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.tenantadmin.CurrentTenantSettingsResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.tenantadmin.TenantAttendanceCaptureFieldsResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.tenantadmin.TenantAttendanceSettingsResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.tenantadmin.UpdateCurrentTenantSettingsRequest;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.tenantadmin.UpdateTenantAttendanceCaptureFieldsRequest;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.tenantadmin.UpdateTenantAttendanceSettingsRequest;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantSettingsEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantSettingsRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TenantSettingsService {

    private final TenantRepository tenantRepository;
    private final TenantSettingsRepository tenantSettingsRepository;
    private final LocalizedMessageService messageService;

    @Transactional(readOnly = true)
    public CurrentTenantSettingsResponse getCurrentTenantSettings() {
        UUID tenantId = requireTenantId();
        return tenantSettingsRepository.findById(tenantId)
                .map(this::toResponse)
                .orElseGet(() -> defaultResponse(tenantId));
    }

    @Transactional
    public CurrentTenantSettingsResponse updateCurrentTenantSettings(UpdateCurrentTenantSettingsRequest request) {
        UUID tenantId = requireTenantId();
        TenantSettingsEntity settings = getOrCreateSettingsForUpdate(tenantId);

        if (request != null && request.getAttendance() != null) {
            applyAttendance(settings, request.getAttendance());
        }

        return toResponse(settings);
    }

    private void applyAttendance(TenantSettingsEntity settings, UpdateTenantAttendanceSettingsRequest attendance) {
        if (attendance.getKioskModeEnabled() != null) {
            settings.setAttendanceKioskModeEnabled(attendance.getKioskModeEnabled());
        }
        if (attendance.getNewcomerCaptureEnabled() != null) {
            settings.setAttendanceNewcomerCaptureEnabled(attendance.getNewcomerCaptureEnabled());
        }

        UpdateTenantAttendanceCaptureFieldsRequest captureFields = attendance.getCaptureFields();
        if (captureFields == null) {
            return;
        }

        if (captureFields.getFullName() != null) {
            settings.setAttendanceCaptureFullName(captureFields.getFullName());
        }
        if (captureFields.getEmail() != null) {
            settings.setAttendanceCaptureEmail(captureFields.getEmail());
        }
        if (captureFields.getPhone() != null) {
            settings.setAttendanceCapturePhone(captureFields.getPhone());
        }
    }

    private CurrentTenantSettingsResponse toResponse(TenantSettingsEntity settings) {
        return CurrentTenantSettingsResponse.builder()
                .tenantId(settings.getTenantId())
                .attendance(TenantAttendanceSettingsResponse.builder()
                        .kioskModeEnabled(settings.isAttendanceKioskModeEnabled())
                        .newcomerCaptureEnabled(settings.isAttendanceNewcomerCaptureEnabled())
                        .captureFields(TenantAttendanceCaptureFieldsResponse.builder()
                                .fullName(settings.isAttendanceCaptureFullName())
                                .email(settings.isAttendanceCaptureEmail())
                                .phone(settings.isAttendanceCapturePhone())
                                .build())
                        .build())
                .build();
    }

    private CurrentTenantSettingsResponse defaultResponse(UUID tenantId) {
        return toResponse(TenantSettingsEntity.builder()
                .tenantId(tenantId)
                .attendanceKioskModeEnabled(false)
                .attendanceNewcomerCaptureEnabled(true)
                .attendanceCaptureFullName(true)
                .attendanceCaptureEmail(true)
                .attendanceCapturePhone(false)
                .build());
    }

    private TenantSettingsEntity getOrCreateSettingsForUpdate(UUID tenantId) {
        return tenantSettingsRepository.findById(tenantId)
                .orElseGet(() -> createSettings(tenantId));
    }

    private TenantSettingsEntity createSettings(UUID tenantId) {
        try {
            return tenantSettingsRepository.saveAndFlush(TenantSettingsEntity.builder()
                    .tenant(resolveTenant(tenantId))
                    .tenantId(tenantId)
                    .build());
        } catch (DataIntegrityViolationException ex) {
            return tenantSettingsRepository.findById(tenantId)
                    .orElseThrow(() -> ex);
        }
    }

    private TenantEntity resolveTenant(UUID tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new EntityNotFoundException(messageService.get(
                        "tenant.notFound",
                        "Tenant not found"
                )));
    }

    private UUID requireTenantId() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException(messageService.get(
                    "tenant.context.missing",
                    "Tenant context is missing"
            ));
        }
        return tenantId;
    }
}
