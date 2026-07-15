package com.anastasia.Anastasia_BackEnd.IntegrationTest.service;

import com.anastasia.Anastasia_BackEnd.TestDataUtil;
import com.anastasia.Anastasia_BackEnd.TestSupport.ServiceIntegrationTestBase;
import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.core.auth.role.RoleType;
import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationChannelType;
import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationDeliveryStatus;
import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationEntity;
import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationType;
import com.anastasia.Anastasia_BackEnd.core.notification.repository.NotificationRepository;
import com.anastasia.Anastasia_BackEnd.core.notification.service.NotificationInboxService;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.Set;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Epic("Integration Tests")
@Feature("Service Layer - Notification Inbox Visibility")
@Transactional
class NotificationInboxVisibilityIT extends ServiceIntegrationTestBase {

    @Autowired private NotificationRepository notificationRepository;
    @Autowired private NotificationInboxService notificationInboxService;

    private UserEntity nonTenantUser;

    @BeforeEach
    void setUpNotificationFixtures() {
        TenantContext.clear();
        nonTenantUser = userRepository.saveAndFlush(UserEntity.builder()
                .fullName("Notification Tester")
                .email("notifications+" + UUID.randomUUID() + "@it.com")
                .password(passwordEncoder.encode(TestDataUtil.TEST_PASSWORD))
                .emailVerifiedAt(Instant.now())
                .roles(Set.of(fetchRole(RoleType.PRIMARY_ADMIN)))
                .build());
        authenticate(nonTenantUser);
    }

    @Test
    void listInbox_exposesSystemNotificationsWithoutTenantAndTenantNotificationsOnlyInMatchingTenant() {
        NotificationEntity systemNotification = createNotification(
                nonTenantUser.getUuid(),
                null,
                "System notice",
                NotificationType.NOTIFICATION
        );
        notificationRepository.saveAndFlush(systemNotification);

        NotificationEntity tenantNotification = createNotification(
                nonTenantUser.getUuid(),
                tenant,
                "Tenant notice",
                NotificationType.EVENT_REMINDER
        );
        notificationRepository.saveAndFlush(tenantNotification);

        TenantContext.clear();

        var noTenantInbox = notificationInboxService.listInbox("ALL", null, 0, 20);
        assertThat(noTenantInbox.items())
                .extracting(item -> item.title())
                .containsExactly("System notice");

        TenantContext.setTenantId(tenant.getId());

        var tenantInbox = notificationInboxService.listInbox("ALL", null, 0, 20);
        assertThat(tenantInbox.items())
                .extracting(item -> item.title())
                .containsExactlyInAnyOrder("System notice", "Tenant notice");
        assertThat(tenantInbox.items())
                .extracting(item -> item.tenantId())
                .containsExactlyInAnyOrder(null, tenant.getId());
    }

    @Test
    void listInbox_rejectsCrossTenantContextForTenantNotifications() {
        NotificationEntity tenantNotification = createNotification(
                nonTenantUser.getUuid(),
                tenant,
                "Tenant notice",
                NotificationType.EVENT_REMINDER
        );
        notificationRepository.saveAndFlush(tenantNotification);

        TenantContext.setTenantId(otherTenant().getId());

        assertThrows(AccessDeniedException.class, () -> notificationInboxService.listInbox("ALL", null, 0, 20));
    }

    private NotificationEntity createNotification(UUID userId, com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity tenantEntity, String title, NotificationType type) {
        NotificationEntity entity = new NotificationEntity();
        entity.setRecipientEmail("notifications@example.com");
        entity.setRecipientUserId(userId);
        entity.setTitle(title);
        entity.setMessage(title + " body");
        entity.setChannel(NotificationChannelType.IN_APP);
        entity.setType(type);
        entity.setDeliveryStatus(NotificationDeliveryStatus.SENT);
        entity.setSentAt(Instant.now());
        entity.setTenant(tenantEntity);
        return entity;
    }

    private com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity otherTenant() {
        return tenantRepository.save(TestDataUtil.createTestTenantEntity());
    }
}
