package com.anastasia.Anastasia_BackEnd.common.filter;

import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.core.auth.principal.UserPrincipal;
import com.anastasia.Anastasia_BackEnd.common.utils.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
/*
TenantFilter is a servlet filter responsible for resolving the tenant identifier for multi-tenant request
processing. It:
1 - Extracts the X-Tenant-ID from the request header, or
2 - Falls back to decoding the tenant ID from the JWT token if the header is absent.
3 - Sets the TenantContext for the duration of the request.
4 - Clears the context after the request is processed.
This enables tenant-based data isolation across the application.
 */
@Component
@RequiredArgsConstructor
public class TenantFilter extends OncePerRequestFilter {
    private static final String PLATFORM_ADMIN_PATH_PREFIX = "/api/v1/platform/admin";
    private static final String PLATFORM_ADMIN_AUTHORITY = "ROLE_PLATFORM_ADMIN";

    private final JwtUtil jwtUtil;
    @Override
    protected void doFilterInternal(HttpServletRequest httpServletRequest,
                                    HttpServletResponse httpServletResponse,
                                    FilterChain filterChain) throws IOException, ServletException {

        if (isPlatformAdminRequest(httpServletRequest)) {
            TenantContext.setTenantId(null);
            try {
                filterChain.doFilter(httpServletRequest, httpServletResponse);
            } finally {
                TenantContext.clear();
            }
            return;
        }

        String tenantIdString = httpServletRequest.getHeader("X-Tenant-ID");
        String alternateTenantHeader = httpServletRequest.getHeader("X-Tenant-Id");
        if ((tenantIdString == null || tenantIdString.isEmpty()) && alternateTenantHeader != null && !alternateTenantHeader.isEmpty()) {
            tenantIdString = alternateTenantHeader;
        }


        if (tenantIdString == null || tenantIdString.isEmpty() || "null".equalsIgnoreCase(tenantIdString)) {
            // Extract tenantId from JWT if not provided in the header
            String authHeader = httpServletRequest.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                try {
                    tenantIdString = jwtUtil.extractTenantId(token);  // Extract from JWT
                } catch (Exception e) {
                    throw new ServletException("Invalid JWT token: " + e.getMessage());
                }
            }
        }

        UUID principalTenantId = resolvePrincipalTenantId();
        if (principalTenantId != null && tenantIdString != null && !tenantIdString.isEmpty() && !"null".equalsIgnoreCase(tenantIdString)) {
            try {
                UUID requestedTenantId = UUID.fromString(tenantIdString);
                if (!principalTenantId.equals(requestedTenantId)) {
                    httpServletResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Tenant access denied");
                    return;
                }
            } catch (IllegalArgumentException e) {
                throw new ServletException("Invalid Tenant ID format");
            }
        }

        if (principalTenantId != null) {
            tenantIdString = principalTenantId.toString();
        }

        if (tenantIdString != null && !tenantIdString.isEmpty() && !"null".equalsIgnoreCase(tenantIdString)) {
            try {
                UUID tenantId = UUID.fromString(tenantIdString);
                TenantContext.setTenantId(tenantId);
            } catch (IllegalArgumentException e) {
                throw new ServletException("Invalid Tenant ID format");
            }
        } else {
//            System.out.println("No tenant is assigned");
            TenantContext.setTenantId(null);  // No tenant assigned
        }

        try {
            filterChain.doFilter(httpServletRequest, httpServletResponse);
        } finally {
            TenantContext.clear();
        }
    }

    private UUID resolvePrincipalTenantId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getTenantId();
        }
        return null;
    }

    private boolean isPlatformAdminRequest(HttpServletRequest request) {
        if (!request.getRequestURI().startsWith(PLATFORM_ADMIN_PATH_PREFIX)) {
            return false;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getAuthorities() != null) {
            boolean hasPlatformAdminAuthority = authentication.getAuthorities().stream()
                    .anyMatch(authority -> PLATFORM_ADMIN_AUTHORITY.equals(authority.getAuthority()));
            if (hasPlatformAdminAuthority) {
                return true;
            }
        }
        return false;
    }


}
