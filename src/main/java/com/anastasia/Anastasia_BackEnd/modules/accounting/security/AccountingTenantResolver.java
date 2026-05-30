package com.anastasia.Anastasia_BackEnd.modules.accounting.security;

import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AccountingTenantResolver {

    public UUID resolveTenant(UUID requestedTenantId) {
        UUID currentTenantId = TenantContext.getTenantId();
        if (currentTenantId == null) {
            throw new AccessDeniedException("Tenant context is required for accounting operations");
        }
        if (requestedTenantId != null && !currentTenantId.equals(requestedTenantId)) {
            throw new AccessDeniedException("Tenant access denied");
        }
        return currentTenantId;
    }
}
