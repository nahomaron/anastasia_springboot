package com.anastasia.Anastasia_BackEnd.UnitTests.groups.support;

import com.anastasia.Anastasia_BackEnd.core.auth.principal.UserPrincipal;
import com.anastasia.Anastasia_BackEnd.modules.groups.service.GroupService;
import com.anastasia.Anastasia_BackEnd.modules.groups.support.GroupSecuritySupport;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupSecuritySupportTest {

    @Mock
    private GroupService groupService;

    @Test
    void canManageGroup_shouldNotGrantAccessFromPriestRoleAlone() {
        GroupSecuritySupport support = new GroupSecuritySupport(groupService);
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UsernamePasswordAuthenticationToken authentication = authentication(userId, "ROLE_PRIEST");

        assertThat(support.canManageGroup(authentication, 10L)).isFalse();
    }

    @Test
    void canManageGroup_shouldAllowExplicitGroupManagementPermission() {
        GroupSecuritySupport support = new GroupSecuritySupport(groupService);
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UsernamePasswordAuthenticationToken authentication = authentication(userId, "MANAGE_REQUESTS");

        assertThat(support.canManageGroup(authentication, 10L)).isTrue();
    }

    @Test
    void canManageGroup_shouldAllowAssignedGroupManager() {
        GroupSecuritySupport support = new GroupSecuritySupport(groupService);
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UsernamePasswordAuthenticationToken authentication = authentication(userId, "ROLE_MEMBER");
        when(groupService.canManageGroup(10L, userId)).thenReturn(true);

        assertThat(support.canManageGroup(authentication, 10L)).isTrue();
    }

    private UsernamePasswordAuthenticationToken authentication(UUID userId, String authority) {
        UserPrincipal principal = new UserPrincipal(UserEntity.builder()
                .uuid(userId)
                .email("user@example.com")
                .build());
        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority(authority))
        );
    }
}
