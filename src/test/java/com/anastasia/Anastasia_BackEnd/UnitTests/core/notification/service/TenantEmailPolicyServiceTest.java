package com.anastasia.Anastasia_BackEnd.UnitTests.core.notification.service;

import com.anastasia.Anastasia_BackEnd.UnitTests.support.LenientMockitoTest;
import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationType;
import com.anastasia.Anastasia_BackEnd.core.notification.repository.NotificationRepository;
import com.anastasia.Anastasia_BackEnd.core.notification.service.TenantEmailFairUseProperties;
import com.anastasia.Anastasia_BackEnd.core.notification.service.TenantEmailPolicyService;
import com.anastasia.Anastasia_BackEnd.core.notification.template.EmailCategory;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.SubscriptionPlan;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantSettingsEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantSubscriptionEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantSettingsRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantSubscriptionRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.entitlement.PlanEntitlementCatalog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@LenientMockitoTest
class TenantEmailPolicyServiceTest {

    @Mock
    private TenantSettingsRepository tenantSettingsRepository;

    @Mock
    private TenantSubscriptionRepository tenantSubscriptionRepository;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private NotificationRepository notificationRepository;

    private TenantEmailPolicyService tenantEmailPolicyService;

    @BeforeEach
    void setUp() {
        TenantEmailFairUseProperties properties = new TenantEmailFairUseProperties();
        tenantEmailPolicyService = new TenantEmailPolicyService(
                tenantSettingsRepository,
                tenantSubscriptionRepository,
                tenantRepository,
                notificationRepository,
                new PlanEntitlementCatalog(),
                properties
        );
    }

    @Test
    void deniesNonExemptEmailWhenTenantIsSuspended() {
        UUID tenantId = UUID.randomUUID();
        TenantSettingsEntity settings = TenantSettingsEntity.builder()
                .tenantId(tenantId)
                .emailSendingSuspended(true)
                .emailSuspensionReason("Complaint spike")
                .build();

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant(tenantId)));
        when(tenantSettingsRepository.findById(tenantId)).thenReturn(Optional.of(settings));
        when(notificationRepository.countSentEmailByTenantAndDeliveredAtBetween(eq(tenantId), any(), any())).thenReturn(12L);

        TenantEmailPolicyService.EmailPolicyDecision decision =
                tenantEmailPolicyService.evaluate(tenantId, EmailCategory.ADMIN_ALERT, NotificationType.NOTIFICATION);

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.errorCode()).isEqualTo(TenantEmailPolicyService.ERROR_CODE_SUSPENDED);
    }

    @Test
    void deniesNonExemptEmailWhenQuotaIsReached() {
        UUID tenantId = UUID.randomUUID();
        TenantSettingsEntity settings = TenantSettingsEntity.builder()
                .tenantId(tenantId)
                .emailMonthlyQuota(5)
                .emailQuotaEnforced(true)
                .build();

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant(tenantId)));
        when(tenantSettingsRepository.findById(tenantId)).thenReturn(Optional.of(settings));
        when(notificationRepository.countSentEmailByTenantAndDeliveredAtBetween(eq(tenantId), any(), any())).thenReturn(5L);

        TenantEmailPolicyService.EmailPolicyDecision decision =
                tenantEmailPolicyService.evaluate(tenantId, EmailCategory.ADMIN_ALERT, NotificationType.NOTIFICATION);

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.errorCode()).isEqualTo(TenantEmailPolicyService.ERROR_CODE_QUOTA_EXCEEDED);
    }

    @Test
    void allowsExemptSecurityEmailEvenWhenQuotaIsReached() {
        UUID tenantId = UUID.randomUUID();
        TenantSettingsEntity settings = TenantSettingsEntity.builder()
                .tenantId(tenantId)
                .emailMonthlyQuota(1)
                .emailQuotaEnforced(true)
                .emailSendingSuspended(true)
                .build();

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant(tenantId)));
        when(tenantSettingsRepository.findById(tenantId)).thenReturn(Optional.of(settings));
        when(notificationRepository.countSentEmailByTenantAndDeliveredAtBetween(eq(tenantId), any(), any())).thenReturn(99L);

        TenantEmailPolicyService.EmailPolicyDecision decision =
                tenantEmailPolicyService.evaluate(tenantId, EmailCategory.SECURITY, NotificationType.PASSWORD_RESET);

        assertThat(decision.allowed()).isTrue();
    }

    @Test
    void usesPlanQuotaWhenTenantOverrideIsMissing() {
        UUID tenantId = UUID.randomUUID();
        TenantSubscriptionEntity subscription = new TenantSubscriptionEntity();
        subscription.setPlan(SubscriptionPlan.ADVANCED);

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant(tenantId)));
        when(tenantSettingsRepository.findById(tenantId)).thenReturn(Optional.empty());
        when(tenantSubscriptionRepository.findByTenantId(tenantId)).thenReturn(Optional.of(subscription));
        when(notificationRepository.countSentEmailByTenantAndDeliveredAtBetween(eq(tenantId), any(), any())).thenReturn(100L);

        TenantEmailPolicyService.EmailUsageSnapshot usage = tenantEmailPolicyService.usageSnapshot(tenantId);

        assertThat(usage.effectiveMonthlyQuota()).isEqualTo(5_000);
        assertThat(usage.currentPeriodSentCount()).isEqualTo(100L);
    }

    private TenantEntity tenant(UUID tenantId) {
        TenantEntity tenant = new TenantEntity();
        tenant.setId(tenantId);
        tenant.setDefaultTimezone("UTC");
        return tenant;
    }
}
