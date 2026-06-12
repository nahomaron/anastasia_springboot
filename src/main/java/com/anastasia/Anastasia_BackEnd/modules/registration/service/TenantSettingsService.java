package com.anastasia.Anastasia_BackEnd.modules.registration.service;

import com.anastasia.Anastasia_BackEnd.common.auditing.AuditEventType;
import com.anastasia.Anastasia_BackEnd.common.auditing.AuditLogService;
import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.common.i18n.LocalizedMessageService;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.tenantadmin.CurrentTenantSettingsResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.tenantadmin.TenantAttendanceCaptureFieldsResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.tenantadmin.TenantAttendanceSettingsResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.tenantadmin.TenantEmailSettingsResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.tenantadmin.TenantSupportAccessSettingsResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.tenantadmin.UpdateCurrentTenantSettingsRequest;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.tenantadmin.UpdateTenantAttendanceCaptureFieldsRequest;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.tenantadmin.UpdateTenantAttendanceSettingsRequest;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.tenantadmin.UpdateTenantEmailSettingsRequest;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.tenantadmin.UpdateTenantSupportAccessSettingsRequest;
import com.anastasia.Anastasia_BackEnd.core.notification.service.TenantEmailPolicyService;
import com.anastasia.Anastasia_BackEnd.modules.platform.admin.service.PlatformSupportAccessService;
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
    private final TenantEmailPolicyService tenantEmailPolicyService;
    private final AuditLogService auditLogService;
    private final PlatformSupportAccessService platformSupportAccessService;

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
        String beforeSummary = summarizeSettings(settings);

        if (request != null && request.getAttendance() != null) {
            applyAttendance(settings, request.getAttendance());
        }
        if (request != null && request.getEmail() != null) {
            applyEmailSettings(settings, request.getEmail());
        }
        if (request != null && request.getSupportAccess() != null) {
            applySupportAccessSettings(settings, request.getSupportAccess());
        }

        auditLogService.record(
                AuditEventType.TENANT_SETTINGS_CHANGED,
                "SUCCESS",
                currentActorUserId(),
                null,
                tenantId,
                "TENANT_SETTINGS",
                tenantId.toString(),
                null,
                "Tenant settings changed from [" + beforeSummary + "] to [" + summarizeSettings(settings) + "]"
        );

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

    private void applyEmailSettings(TenantSettingsEntity settings, UpdateTenantEmailSettingsRequest email) {
        if (email.getQuotaEnforced() != null) {
            settings.setEmailQuotaEnforced(email.getQuotaEnforced());
        }
        if (email.getSendingSuspended() != null) {
            settings.setEmailSendingSuspended(email.getSendingSuspended());
            if (!email.getSendingSuspended() && email.getSuspensionReason() == null) {
                settings.setEmailSuspensionReason(null);
            }
        }
        if (email.getMonthlyQuota() != null) {
            settings.setEmailMonthlyQuota(email.getMonthlyQuota());
        }
        if (email.getSuspensionReason() != null) {
            String trimmed = email.getSuspensionReason().trim();
            settings.setEmailSuspensionReason(trimmed.isEmpty() ? null : trimmed);
        }
    }

    private void applySupportAccessSettings(TenantSettingsEntity settings, UpdateTenantSupportAccessSettingsRequest supportAccess) {
        if (supportAccess.getEnabled() != null) {
            settings.setSupportAccessEnabled(supportAccess.getEnabled());
        }
    }

    private CurrentTenantSettingsResponse toResponse(TenantSettingsEntity settings) {
        TenantEmailPolicyService.EmailUsageSnapshot emailUsage = tenantEmailPolicyService.usageSnapshot(settings.getTenantId());
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
                .email(TenantEmailSettingsResponse.builder()
                        .quotaEnforced(settings.isEmailQuotaEnforced())
                        .sendingSuspended(settings.isEmailSendingSuspended())
                        .suspensionReason(settings.getEmailSuspensionReason())
                        .monthlyQuota(settings.getEmailMonthlyQuota())
                        .effectiveMonthlyQuota(emailUsage.effectiveMonthlyQuota())
                        .currentPeriodSentCount(emailUsage.currentPeriodSentCount())
                        .currentPeriodStart(emailUsage.currentPeriodStart())
                        .currentPeriodEnd(emailUsage.currentPeriodEnd())
                        .build())
                .supportAccess(TenantSupportAccessSettingsResponse.builder()
                        .enabled(settings.isSupportAccessEnabled())
                        .recentHistory(platformSupportAccessService.listRecentTenantHistory(settings.getTenantId()))
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
                .emailQuotaEnforced(true)
                .emailSendingSuspended(false)
                .supportAccessEnabled(true)
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

    private UUID currentActorUserId() {
        var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof com.anastasia.Anastasia_BackEnd.core.auth.principal.UserPrincipal principal) {
            return principal.getUserUuid();
        }
        return null;
    }

    private String summarizeSettings(TenantSettingsEntity settings) {
        return "attendance.kiosk=" + settings.isAttendanceKioskModeEnabled()
                + ", attendance.newcomerCapture=" + settings.isAttendanceNewcomerCaptureEnabled()
                + ", attendance.captureFullName=" + settings.isAttendanceCaptureFullName()
                + ", attendance.captureEmail=" + settings.isAttendanceCaptureEmail()
                + ", attendance.capturePhone=" + settings.isAttendanceCapturePhone()
                + ", email.quotaEnforced=" + settings.isEmailQuotaEnforced()
                + ", email.sendingSuspended=" + settings.isEmailSendingSuspended()
                + ", email.monthlyQuota=" + settings.getEmailMonthlyQuota()
                + ", email.suspensionReason=" + settings.getEmailSuspensionReason()
                + ", supportAccess.enabled=" + settings.isSupportAccessEnabled();
    }
}
