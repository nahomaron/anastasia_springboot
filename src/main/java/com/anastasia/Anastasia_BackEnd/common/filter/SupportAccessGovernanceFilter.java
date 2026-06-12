package com.anastasia.Anastasia_BackEnd.common.filter;

import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.core.auth.principal.UserPrincipal;
import com.anastasia.Anastasia_BackEnd.modules.platform.admin.model.SupportAccessActionType;
import com.anastasia.Anastasia_BackEnd.modules.platform.admin.model.SupportAccessScope;
import com.anastasia.Anastasia_BackEnd.modules.platform.admin.model.SupportAccessSessionEntity;
import com.anastasia.Anastasia_BackEnd.modules.platform.admin.service.PlatformSupportAccessService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class SupportAccessGovernanceFilter extends OncePerRequestFilter {

    private static final Pattern PLATFORM_SUBSCRIPTION_TENANT_PATTERN =
            Pattern.compile("^/api/v1/platform/subscriptions/([0-9a-fA-F\\-]{36})(/.*)?$");
    private static final Pattern PLATFORM_DEMO_TEMPLATE_TENANT_PATTERN =
            Pattern.compile("^/api/v1/platform/subscriptions/demo-template/([0-9a-fA-F\\-]{36})$");
    private static final Pattern PLATFORM_TENANT_STATUS_PATTERN =
            Pattern.compile("^/api/v1/platform/admin/tenants/([0-9a-fA-F\\-]{36})/status$");

    private final PlatformSupportAccessService platformSupportAccessService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        AuthContext authContext = resolveAuthContext();
        if (authContext == null || !authContext.platformAdmin()) {
            filterChain.doFilter(request, response);
            return;
        }

        UUID governedTenantId = resolveGovernedTenantId(request);
        if (governedTenantId == null) {
            filterChain.doFilter(request, response);
            return;
        }

        UUID sessionId = resolveSessionId(request);
        if (sessionId == null) {
            throw new AccessDeniedException("An active support access session header is required.");
        }

        SupportAccessSessionEntity session = platformSupportAccessService.authorizeSession(
                authContext.userId(),
                governedTenantId,
                sessionId,
                isWriteMethod(request.getMethod()) ? SupportAccessScope.READ_WRITE : SupportAccessScope.READ_ONLY
        );

        int responseStatus = HttpServletResponse.SC_OK;
        String detail = null;
        try {
            filterChain.doFilter(request, response);
            responseStatus = response.getStatus();
        } catch (ServletException | IOException ex) {
            responseStatus = response.getStatus() >= 400 ? response.getStatus() : HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            detail = ex.getClass().getSimpleName();
            throw ex;
        } catch (RuntimeException ex) {
            responseStatus = ex instanceof AccessDeniedException ? HttpServletResponse.SC_FORBIDDEN : HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            detail = ex.getClass().getSimpleName();
            throw ex;
        } finally {
            platformSupportAccessService.recordAction(
                    session,
                    request.getRequestURI().startsWith("/api/v1/platform/") ? SupportAccessActionType.PLATFORM_ADMIN_ACTION : SupportAccessActionType.TENANT_API_REQUEST,
                    request.getMethod(),
                    request.getRequestURI(),
                    responseStatus,
                    detail
            );
        }
    }

    private AuthContext resolveAuthContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return null;
        }
        boolean platformAdmin = principal.getRoleNames().contains("PLATFORM_ADMIN")
                || principal.getRoleNames().contains("DEVELOPER_SUPER_USER");
        return new AuthContext(principal.getUserUuid(), platformAdmin);
    }

    private UUID resolveGovernedTenantId(HttpServletRequest request) {
        UUID tenantContextId = TenantContext.getTenantId();
        if (tenantContextId != null) {
            return tenantContextId;
        }

        String uri = request.getRequestURI();
        Matcher subscriptions = PLATFORM_SUBSCRIPTION_TENANT_PATTERN.matcher(uri);
        if (subscriptions.matches()) {
            return UUID.fromString(subscriptions.group(1));
        }

        Matcher demoTemplate = PLATFORM_DEMO_TEMPLATE_TENANT_PATTERN.matcher(uri);
        if (demoTemplate.matches()) {
            return UUID.fromString(demoTemplate.group(1));
        }

        Matcher tenantStatus = PLATFORM_TENANT_STATUS_PATTERN.matcher(uri);
        if (tenantStatus.matches()) {
            return UUID.fromString(tenantStatus.group(1));
        }
        return null;
    }

    private UUID resolveSessionId(HttpServletRequest request) {
        String raw = request.getHeader(PlatformSupportAccessService.SESSION_HEADER);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw.trim());
        } catch (IllegalArgumentException ex) {
            throw new AccessDeniedException("Invalid support access session header.");
        }
    }

    private boolean isWriteMethod(String method) {
        return !"GET".equalsIgnoreCase(method)
                && !"HEAD".equalsIgnoreCase(method)
                && !"OPTIONS".equalsIgnoreCase(method);
    }

    private record AuthContext(UUID userId, boolean platformAdmin) {
    }
}
