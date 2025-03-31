package com.anastasia.Anastasia_BackEnd.filter;

import com.anastasia.Anastasia_BackEnd.config.TenantContext;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
public class TenantFilter implements Filter {

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        String tenantIdString = httpServletRequest.getHeader("X-Tenant-ID");

        if (tenantIdString != null && !tenantIdString.isEmpty()) {
            try {
                // Attempt to convert the string to UUID
                UUID tenantId = UUID.fromString(tenantIdString);
                TenantContext.setTenantId(tenantId);
            } catch (IllegalArgumentException e) {
                // If the string is not a valid UUID, log the error or handle it as needed
                throw new ServletException("Invalid Tenant ID format");
            }
        }

        try {
            filterChain.doFilter(servletRequest, servletResponse);
        } finally {
            TenantContext.clear();
        }
    }

}
