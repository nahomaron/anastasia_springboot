package com.anastasia.Anastasia_BackEnd.UnitTests.controller;

import com.anastasia.Anastasia_BackEnd.UnitTests.support.LenientMockitoTest;
import com.anastasia.Anastasia_BackEnd.common.utils.RateLimiterService;
import com.anastasia.Anastasia_BackEnd.core.auth.audit.PlatformAdminBootstrapAuditOutcome;
import com.anastasia.Anastasia_BackEnd.core.auth.audit.PlatformAdminBootstrapAuditService;
import com.anastasia.Anastasia_BackEnd.core.auth.controller.PlatformAdminRegistrationController;
import com.anastasia.Anastasia_BackEnd.core.auth.dto.PlatformAdminRegistrationRequest;
import com.anastasia.Anastasia_BackEnd.core.auth.service.PlatformAdminRegistrationService;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@LenientMockitoTest
class PlatformAdminRegistrationControllerUnitTest {

    @Mock
    private PlatformAdminRegistrationService platformAdminRegistrationService;

    @Mock
    private PlatformAdminBootstrapAuditService auditService;

    @Mock
    private RateLimiterService rateLimiterService;

    @Mock
    private HttpServletRequest httpRequest;

    @InjectMocks
    private PlatformAdminRegistrationController controller;

    @Test
    void register_usesRemoteAddrInsteadOfSpoofableForwardedHeader() {
        PlatformAdminRegistrationRequest request = new PlatformAdminRegistrationRequest();
        request.setEmail("Bootstrap@Example.com");
        request.setFullName("Bootstrap Admin");
        request.setPassword("StrongPass123!");

        UserEntity createdUser = UserEntity.builder()
                .uuid(UUID.randomUUID())
                .build();

        when(httpRequest.getRemoteAddr()).thenReturn("198.51.100.24");
        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn("203.0.113.10");
        when(httpRequest.getHeader("User-Agent")).thenReturn("JUnit");
        when(rateLimiterService.tryConsume(anyString(), anyLong(), any(Duration.class))).thenReturn(true);
        when(platformAdminRegistrationService.registerPlatformAdmin(request, "dev-secret")).thenReturn(createdUser);

        var response = controller.register(request, "dev-secret", httpRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(Map.of(
                "message", "Platform admin bootstrap completed successfully",
                "userId", createdUser.getUuid().toString()
        ));
        verify(rateLimiterService).tryConsume(
                "auth:platform-admin-bootstrap:198.51.100.24:bootstrap@example.com",
                3L,
                Duration.ofMinutes(15)
        );
        verify(auditService).recordAttempt(
                "bootstrap@example.com",
                PlatformAdminBootstrapAuditOutcome.SUCCESS,
                "Platform admin bootstrap completed successfully",
                "198.51.100.24",
                "JUnit",
                createdUser.getUuid()
        );
    }
}
