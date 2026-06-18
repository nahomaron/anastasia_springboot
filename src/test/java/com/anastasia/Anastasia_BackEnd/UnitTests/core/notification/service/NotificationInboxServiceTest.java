package com.anastasia.Anastasia_BackEnd.UnitTests.core.notification.service;

import com.anastasia.Anastasia_BackEnd.UnitTests.support.LenientMockitoTest;
import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.core.auth.principal.UserPrincipal;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import com.anastasia.Anastasia_BackEnd.core.notification.repository.NotificationPreferenceRepository;
import com.anastasia.Anastasia_BackEnd.core.notification.repository.NotificationRepository;
import com.anastasia.Anastasia_BackEnd.core.notification.service.NotificationInboxService;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantFeature;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.entitlement.TenantEntitlementAccessService;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@LenientMockitoTest
class NotificationInboxServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationPreferenceRepository preferenceRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TenantEntitlementAccessService entitlementAccessService;

    @InjectMocks
    private NotificationInboxService notificationInboxService;

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @Test
    void unreadCount_shouldUseAuthenticationNameWhenPrincipalIsNotUserPrincipal() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UserEntity user = tenantUser(userId, tenantId, "member@example.com");

        when(userRepository.findByEmailIgnoreCase("member@example.com")).thenReturn(Optional.of(user));
        when(notificationRepository.countUnread(tenantId, userId)).thenReturn(7L);
        doNothing().when(entitlementAccessService).requireFeature(TenantFeature.NOTIFICATIONS);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("member@example.com", null, List.of())
        );

        long unreadCount = notificationInboxService.unreadCount();

        assertThat(unreadCount).isEqualTo(7L);
        verify(notificationRepository).countUnread(tenantId, userId);
    }

    @Test
    void unreadCount_shouldFallbackToPrincipalTenantWhenTenantContextMissing() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UserEntity user = tenantUser(userId, tenantId, "member@example.com");
        UserPrincipal principal = new UserPrincipal(user);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(notificationRepository.countUnread(tenantId, userId)).thenReturn(3L);
        doNothing().when(entitlementAccessService).requireFeature(TenantFeature.NOTIFICATIONS);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );

        long unreadCount = notificationInboxService.unreadCount();

        assertThat(unreadCount).isEqualTo(3L);
        verify(notificationRepository).countUnread(tenantId, userId);
    }

    @Test
    void unreadCount_shouldRejectCrossTenantAccess() {
        UUID principalTenantId = UUID.randomUUID();
        UUID persistedTenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UserEntity principalUser = tenantUser(userId, principalTenantId, "member@example.com");
        UserEntity persistedUser = tenantUser(userId, persistedTenantId, "member@example.com");
        UserPrincipal principal = new UserPrincipal(principalUser);

        when(userRepository.findById(userId)).thenReturn(Optional.of(persistedUser));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );

        assertThrows(AccessDeniedException.class, () -> notificationInboxService.unreadCount());
    }

    private UserEntity tenantUser(UUID userId, UUID tenantId, String email) {
        TenantEntity tenant = TenantEntity.builder().id(tenantId).build();
        return UserEntity.builder()
                .uuid(userId)
                .email(email)
                .fullName("Test User")
                .affiliatedTenant(tenant)
                .build();
    }
}
