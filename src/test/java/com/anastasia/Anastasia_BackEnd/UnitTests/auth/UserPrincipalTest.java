package com.anastasia.Anastasia_BackEnd.UnitTests.auth;

import com.anastasia.Anastasia_BackEnd.core.auth.principal.UserPrincipal;
import com.anastasia.Anastasia_BackEnd.core.auth.role.Role;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserStatus;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserType;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserPrincipalTest {

    @Test
    void builtInTenantRolesContributeDefaultAuthoritiesWithoutPersistedRolePermissions() {
        UserEntity user = UserEntity.builder()
                .uuid(UUID.randomUUID())
                .email("nahomtemeharay@gmail.com")
                .fullName("Nahom Temeharay")
                .password("secret")
                .userType(UserType.TENANT)
                .status(UserStatus.ACTIVE)
                .build();

        Role ownerRole = Role.builder()
                .roleName("OWNER")
                .build();
        Role primaryAdminRole = Role.builder()
                .roleName("PRIMARY_ADMIN")
                .build();

        UserPrincipal principal = new UserPrincipal(user, Set.of(ownerRole, primaryAdminRole), Set.of());

        Set<String> authorities = principal.getAuthorities().stream()
                .map(grantedAuthority -> grantedAuthority.getAuthority())
                .collect(java.util.stream.Collectors.toSet());

        assertThat(authorities)
                .contains(
                        "ROLE_OWNER",
                        "ROLE_PRIMARY_ADMIN",
                        "OWN_SUBSCRIPTION",
                        "MANAGE_TENANT_BILLING",
                        "MANAGE_ROLES",
                        "VIEW_TENANT_USERS",
                        "MANAGE_FINANCE"
                );
    }
}
