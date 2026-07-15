package com.anastasia.Anastasia_BackEnd.UnitTests.controller;

import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.common.i18n.LocalizedMessageService;
import com.anastasia.Anastasia_BackEnd.modules.registration.controller.PriestController;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.ChildService;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.MemberService;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.PriestService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class PriestControllerTenantOverrideTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @Test
    void resolveTenantId_shouldRejectTenantOverrideForTenantScopedUser() {
        UUID contextTenantId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID requestedTenantId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        TenantContext.setTenantId(contextTenantId);
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("user", "pw", "VIEW_PRIEST_ASSIGNMENTS")
        );

        PriestController controller = controller();

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(controller, "resolveTenantId", requestedTenantId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Cross-tenant priest assignment lookup requires platform authority");
    }

    @Test
    void resolveTenantId_shouldAllowTenantOverrideForPlatformReadAuthority() {
        UUID contextTenantId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID requestedTenantId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        TenantContext.setTenantId(contextTenantId);
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("user", "pw", "VIEW_ALL_DATA")
        );

        PriestController controller = controller();

        UUID resolved = ReflectionTestUtils.invokeMethod(controller, "resolveTenantId", requestedTenantId);

        assertThat(resolved).isEqualTo(requestedTenantId);
    }

    private PriestController controller() {
        return new PriestController(
                mock(PriestService.class),
                mock(MemberService.class),
                mock(ChildService.class),
                mock(LocalizedMessageService.class),
                new ObjectMapper()
        );
    }
}
