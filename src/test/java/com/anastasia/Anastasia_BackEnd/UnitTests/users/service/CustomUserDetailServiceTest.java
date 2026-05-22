package com.anastasia.Anastasia_BackEnd.UnitTests.users.service;

import com.anastasia.Anastasia_BackEnd.common.i18n.LocalizedMessageService;
import com.anastasia.Anastasia_BackEnd.core.auth.principal.UserPrincipal;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.RoleRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.role.Role;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantAdminAssignmentRepository;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.repository.TenantUserPermissionGrantRepository;
import com.anastasia.Anastasia_BackEnd.modules.users.service.CustomUserDetailService;
import com.anastasia.Anastasia_BackEnd.modules.users.service.TenantUserAccessPolicy;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomUserDetailServiceTest {

    @Test
    void loadUserByUsername_preservesPlatformAdminAuthorityForTenantlessUser() {
        UserRepository userRepository = mock(UserRepository.class);
        RoleRepository roleRepository = mock(RoleRepository.class);
        TenantAdminAssignmentRepository assignmentRepository = mock(TenantAdminAssignmentRepository.class);
        TenantUserPermissionGrantRepository permissionGrantRepository = mock(TenantUserPermissionGrantRepository.class);
        LocalizedMessageService messageService = mock(LocalizedMessageService.class);

        CustomUserDetailService service = new CustomUserDetailService(
                userRepository,
                roleRepository,
                assignmentRepository,
                permissionGrantRepository,
                new TenantUserAccessPolicy(),
                messageService
        );

        Role platformAdminRole = Role.builder()
                .id(10L)
                .roleName("PLATFORM_ADMIN")
                .build();
        platformAdminRole.setTenantId(UUID.randomUUID());

        UserEntity user = UserEntity.builder()
                .uuid(UUID.randomUUID())
                .email("platform-admin@example.com")
                .password("secret")
                .fullName("Platform Admin")
                .roles(Set.of(platformAdminRole))
                .build();

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(permissionGrantRepository.findByUserIdAndTenantId(user.getUuid(), null)).thenReturn(java.util.List.of());

        UserPrincipal principal = (UserPrincipal) service.loadUserByUsername(user.getEmail());

        assertThat(principal.getRoleNames()).contains("PLATFORM_ADMIN");
        assertThat(principal.getAuthorities())
                .extracting(authority -> authority.getAuthority())
                .contains("ROLE_PLATFORM_ADMIN");
    }
}
