package com.anastasia.Anastasia_BackEnd.core.notification.service;

import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationType;
import com.anastasia.Anastasia_BackEnd.core.notification.repository.NotificationRepository;
import com.anastasia.Anastasia_BackEnd.core.notification.template.EmailCategory;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.SubscriptionPlan;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantSettingsEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantSettingsRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantSubscriptionRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.entitlement.PlanEntitlementCatalog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TenantEmailPolicyService {

    public static final String ERROR_CODE_SUSPENDED = "TENANT_EMAIL_SUSPENDED";
    public static final String ERROR_CODE_QUOTA_EXCEEDED = "TENANT_EMAIL_QUOTA_EXCEEDED";

    private final TenantSettingsRepository tenantSettingsRepository;
    private final TenantSubscriptionRepository tenantSubscriptionRepository;
    private final TenantRepository tenantRepository;
    private final NotificationRepository notificationRepository;
    private final PlanEntitlementCatalog planEntitlementCatalog;
    private final TenantEmailFairUseProperties properties;

    public EmailPolicyDecision evaluate(UUID tenantId, EmailCategory category, NotificationType notificationType) {
        if (tenantId == null || !properties.isEnabled()) {
            return EmailPolicyDecision.allow(null);
        }

        Instant now = Instant.now();
        TenantEntity tenant = tenantRepository.findById(tenantId).orElse(null);
        TenantSettingsEntity settings = tenantSettingsRepository.findById(tenantId).orElse(null);
        EmailUsageSnapshot usage = usageSnapshot(tenantId, tenant, settings, now);
        boolean exempt = isExempt(category, notificationType);

        if (!exempt && settings != null && settings.isEmailSendingSuspended()) {
            String reason = StringUtils.hasText(settings.getEmailSuspensionReason())
                    ? settings.getEmailSuspensionReason().trim()
                    : "Tenant email sending is suspended";
            return EmailPolicyDecision.deny(ERROR_CODE_SUSPENDED, reason, usage);
        }

        if (!exempt
                && usage.quotaEnforced()
                && usage.effectiveMonthlyQuota() != null
                && usage.effectiveMonthlyQuota() >= 0
                && usage.currentPeriodSentCount() >= usage.effectiveMonthlyQuota()) {
            return EmailPolicyDecision.deny(
                    ERROR_CODE_QUOTA_EXCEEDED,
                    "Tenant email monthly quota has been reached",
                    usage
            );
        }

        return EmailPolicyDecision.allow(usage);
    }

    public EmailUsageSnapshot usageSnapshot(UUID tenantId) {
        TenantEntity tenant = tenantRepository.findById(tenantId).orElse(null);
        TenantSettingsEntity settings = tenantSettingsRepository.findById(tenantId).orElse(null);
        return usageSnapshot(tenantId, tenant, settings, Instant.now());
    }

    private boolean isExempt(EmailCategory category, NotificationType notificationType) {
        if (category != null && properties.getExemptCategories().contains(category)) {
            return true;
        }
        return notificationType == NotificationType.ACCOUNT_ACTIVATION
                || notificationType == NotificationType.PASSWORD_RESET;
    }

    private EmailUsageSnapshot usageSnapshot(UUID tenantId,
                                             TenantEntity tenant,
                                             TenantSettingsEntity settings,
                                             Instant now) {
        ZoneId zoneId = resolveZoneId(tenant);
        LocalDate currentDate = ZonedDateTime.ofInstant(now, zoneId).toLocalDate();
        Instant periodStart = currentDate.withDayOfMonth(1).atStartOfDay(zoneId).toInstant();
        Instant periodEnd = currentDate.withDayOfMonth(1).plusMonths(1).atStartOfDay(zoneId).toInstant();
        Integer effectiveMonthlyQuota = resolveEffectiveMonthlyQuota(settings, tenantId);
        long currentPeriodSentCount = notificationRepository.countSentEmailByTenantAndDeliveredAtBetween(
                tenantId,
                periodStart,
                periodEnd
        );

        return new EmailUsageSnapshot(
                currentPeriodSentCount,
                periodStart,
                periodEnd,
                effectiveMonthlyQuota,
                settings == null || settings.isEmailQuotaEnforced(),
                settings != null && settings.isEmailSendingSuspended(),
                settings != null ? settings.getEmailSuspensionReason() : null
        );
    }

    private Integer resolveEffectiveMonthlyQuota(TenantSettingsEntity settings, UUID tenantId) {
        if (settings != null && settings.getEmailMonthlyQuota() != null) {
            return settings.getEmailMonthlyQuota();
        }

        SubscriptionPlan plan = tenantSubscriptionRepository.findByTenantId(tenantId)
                .map(subscription -> subscription.getPlan())
                .orElse(SubscriptionPlan.FREE);
        return properties.resolvePlanQuota(plan, planEntitlementCatalog);
    }

    private ZoneId resolveZoneId(TenantEntity tenant) {
        if (tenant == null || !StringUtils.hasText(tenant.getDefaultTimezone())) {
            return ZoneOffset.UTC;
        }
        try {
            return ZoneId.of(tenant.getDefaultTimezone().trim());
        } catch (Exception ex) {
            return ZoneOffset.UTC;
        }
    }

    public record EmailPolicyDecision(boolean allowed,
                                      String errorCode,
                                      String errorMessage,
                                      EmailUsageSnapshot usage) {

        static EmailPolicyDecision allow(EmailUsageSnapshot usage) {
            return new EmailPolicyDecision(true, null, null, usage);
        }

        static EmailPolicyDecision deny(String errorCode, String errorMessage, EmailUsageSnapshot usage) {
            return new EmailPolicyDecision(false, errorCode, errorMessage, usage);
        }
    }

    public record EmailUsageSnapshot(long currentPeriodSentCount,
                                     Instant currentPeriodStart,
                                     Instant currentPeriodEnd,
                                     Integer effectiveMonthlyQuota,
                                     boolean quotaEnforced,
                                     boolean sendingSuspended,
                                     String suspensionReason) {
    }
}
