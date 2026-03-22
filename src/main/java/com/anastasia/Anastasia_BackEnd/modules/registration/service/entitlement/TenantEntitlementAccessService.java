package com.anastasia.Anastasia_BackEnd.modules.registration.service.entitlement;

import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantFeature;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TenantEntitlementAccessService {

    private final EntitlementResolverService entitlementResolverService;
    private final SubscriptionService subscriptionService;

    public void requireFeature(TenantFeature feature) {
        if (isPlatformAdmin()) {
            return;
        }
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            return;
        }
        subscriptionService.syncSubscriptionState(tenantId, null);
        boolean allowed = entitlementResolverService.hasFeature(tenantId, feature);
        if (!allowed) {
            throw new AccessDeniedException("Feature not available for current tenant plan: " + feature.name());
        }
    }

    public int activeMembersLimit(UUID tenantId) {
        return entitlementResolverService.activeMembersLimit(tenantId);
    }

    private boolean isPlatformAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_PLATFORM_ADMIN"::equals);
    }
}
