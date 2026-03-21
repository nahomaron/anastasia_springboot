package com.anastasia.Anastasia_BackEnd.modules.users.service;

import com.anastasia.Anastasia_BackEnd.core.auth.role.Role;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserType;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class TenantUserAccessPolicy {

    private static final Set<String> GOVERNANCE_ROLE_NAMES = Set.of(
            "OWNER",
            "ADMIN",
            "PRIMARY_ADMIN",
            "PLATFORM_ADMIN"
    );

    public boolean isProtectedAccount(UserEntity user) {
        if (user == null) {
            return false;
        }

        if (UserType.TENANT.equals(user.getUserType())) {
            return true;
        }

        Set<String> roleNames = user.getRoles().stream()
                .map(Role::getRoleName)
                .filter(name -> name != null && !name.isBlank())
                .collect(Collectors.toSet());

        return roleNames.contains("OWNER")
                || roleNames.contains("ADMIN")
                || roleNames.contains("PRIMARY_ADMIN")
                || roleNames.contains("PLATFORM_ADMIN");
    }

    public String protectedReason(UserEntity user) {
        if (!isProtectedAccount(user)) {
            return null;
        }

        if (UserType.TENANT.equals(user.getUserType())) {
            return "Tenant governance account";
        }

        Set<String> roleNames = user.getRoles().stream()
                .map(Role::getRoleName)
                .filter(name -> name != null && !name.isBlank())
                .collect(Collectors.toSet());

        if (roleNames.contains("OWNER") && roleNames.contains("ADMIN")) {
            return "Owner/Admin governance role";
        }
        if (roleNames.contains("PRIMARY_ADMIN")) {
            return "Primary admin governance role";
        }
        if (roleNames.contains("PLATFORM_ADMIN")) {
            return "Platform admin governance role";
        }
        if (roleNames.contains("OWNER")) {
            return "Owner governance role";
        }
        return "Admin governance role";
    }

    public boolean isRoleVisibleForTenant(Role role, UUID tenantId) {
        if (role == null) {
            return false;
        }
        return role.getTenantId() == null || role.getTenantId().equals(tenantId);
    }

    public boolean isAssignableThroughTenantAccess(Role role, UUID tenantId) {
        return isRoleVisibleForTenant(role, tenantId)
                && role.getRoleName() != null
                && !GOVERNANCE_ROLE_NAMES.contains(role.getRoleName());
    }

    public Set<Role> explicitRolesForTenant(UserEntity user, UUID tenantId) {
        if (user == null || user.getRoles() == null) {
            return Set.of();
        }

        return user.getRoles().stream()
                .filter(role -> isRoleVisibleForTenant(role, tenantId))
                .sorted(Comparator.comparing(Role::getRoleName, Comparator.nullsLast(String::compareTo)))
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
    }
}
