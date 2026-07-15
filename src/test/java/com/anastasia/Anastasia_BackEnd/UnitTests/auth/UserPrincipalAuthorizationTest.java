package com.anastasia.Anastasia_BackEnd.UnitTests.auth;

import com.anastasia.Anastasia_BackEnd.core.auth.permission.Permission;
import com.anastasia.Anastasia_BackEnd.core.auth.permission.PermissionType;
import com.anastasia.Anastasia_BackEnd.core.auth.principal.UserPrincipal;
import com.anastasia.Anastasia_BackEnd.core.auth.role.Role;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserPrincipalAuthorizationTest {

    @Test
    void userPrincipal_shouldExposeRoleAndPermissionAuthorities() {
        Role role = Role.builder()
                .roleName("PRIEST")
                .permissions(Set.of(Permission.builder().name(PermissionType.VIEW_CALENDAR).build()))
                .build();

        UserEntity user = UserEntity.builder()
                .uuid(UUID.randomUUID())
                .email("priest@example.com")
                .password("Password1!")
                .roles(Set.of(role))
                .build();

        UserPrincipal principal = new UserPrincipal(user);

        assertThat(principal.getRoleNames()).contains("PRIEST");
        assertThat(principal.hasPermission("ROLE_PRIEST")).isTrue();
        assertThat(principal.hasPermission("VIEW_CALENDAR")).isTrue();
        assertThat(principal.hasPermission("MANAGE_CALENDAR")).isFalse();
    }
}
