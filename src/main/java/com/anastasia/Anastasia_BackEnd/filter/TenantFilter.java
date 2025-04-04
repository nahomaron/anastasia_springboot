package com.anastasia.Anastasia_BackEnd.filter;

import com.anastasia.Anastasia_BackEnd.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.util.JwtUtil;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TenantFilter implements Filter {

    private final JwtUtil jwtUtil;
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        String tenantIdString = httpServletRequest.getHeader("X-Tenant-ID");


        if (tenantIdString == null || tenantIdString.isEmpty()) {
            // Extract tenantId from JWT if not provided in the header
            String authHeader = httpServletRequest.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                try {
                    tenantIdString = jwtUtil.extractTenantId(token);  // Extract from JWT
                    if ("null".equals(tenantIdString)) {
                        tenantIdString = null; // Treat "null" as an actual null
                    }
                } catch (Exception e) {
                    throw new ServletException("Invalid JWT token: " + e.getMessage());
                }
            }
        }

        if (tenantIdString != null && !tenantIdString.isEmpty()) {
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
            filterChain.doFilter(servletRequest, servletResponse);
        } finally {
            TenantContext.clear();
        }
    }

}
