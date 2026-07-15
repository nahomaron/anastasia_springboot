package com.anastasia.Anastasia_BackEnd.UnitTests.controller;

import com.anastasia.Anastasia_BackEnd.common.i18n.LocalizedMessageService;
import com.anastasia.Anastasia_BackEnd.common.utils.RateLimiterService;
import com.anastasia.Anastasia_BackEnd.core.auth.principal.UserPrincipal;
import com.anastasia.Anastasia_BackEnd.modules.registration.controller.TenantController;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.TenantService;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TenantControllerTenantAccessTest {

    private final TenantService tenantService = mock(TenantService.class);
    private final RateLimiterService rateLimiterService = mock(RateLimiterService.class);
    private final LocalizedMessageService messageService = mock(LocalizedMessageService.class);
    private final TenantController controller = new TenantController(tenantService, rateLimiterService, messageService);

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void updateTenant_shouldRejectTenantScopedUserForDifferentTenant() {
        UUID currentTenantId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID requestedTenantId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        authenticate(currentTenantId, "OWN_SUBSCRIPTION");
        when(messageService.get(anyString(), anyString())).thenAnswer(invocation -> invocation.getArgument(1));

        assertThatThrownBy(() -> controller.updateTenant(requestedTenantId, new TenantDTO()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Tenant access denied");
    }

    @Test
    void updateTenant_shouldAllowTenantScopedUserForOwnTenant() {
        UUID currentTenantId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        authenticate(currentTenantId, "OWN_SUBSCRIPTION");

        controller.updateTenant(currentTenantId, new TenantDTO());

        verify(tenantService).updateTenant(org.mockito.ArgumentMatchers.eq(currentTenantId), any(TenantDTO.class));
    }

    @Test
    void unsubscribeTenant_shouldAllowPlatformAuthorityForAnyTenant() {
        UUID currentTenantId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID requestedTenantId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        authenticate(currentTenantId, "MANAGE_TENANTS");

        controller.unsubscribeTenant(requestedTenantId);

        verify(tenantService).unsubscribeTenant(requestedTenantId);
    }

    private void authenticate(UUID tenantId, String authority) {
        TenantEntity tenant = TenantEntity.builder()
                .id(tenantId)
                .build();
        UserEntity user = UserEntity.builder()
                .uuid(UUID.randomUUID())
                .email("owner@example.com")
                .affiliatedTenant(tenant)
                .build();
        UserPrincipal principal = new UserPrincipal(user);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority(authority))
        ));
    }
}
