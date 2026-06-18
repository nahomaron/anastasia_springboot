package com.anastasia.Anastasia_BackEnd.UnitTests.users.service;

import com.anastasia.Anastasia_BackEnd.common.i18n.LocalizedMessageService;
import com.anastasia.Anastasia_BackEnd.core.auth.principal.UserPrincipal;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.RoleRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.role.Role;
import com.anastasia.Anastasia_BackEnd.core.auth.service.MemberEffectivePermissionService;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantAdminAssignmentRepository;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.repository.TenantUserPermissionGrantRepository;
import com.anastasia.Anastasia_BackEnd.modules.users.service.CustomUserDetailService;
import com.anastasia.Anastasia_BackEnd.modules.users.service.TenantUserAccessPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.InvalidDataAccessResourceUsageException;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class CustomUserDetailServiceTest {

    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private TenantAdminAssignmentRepository assignmentRepository;
    private TenantUserPermissionGrantRepository permissionGrantRepository;
    private LocalizedMessageService messageService;
    private MemberEffectivePermissionService memberEffectivePermissionService;
    private CustomUserDetailService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        roleRepository = mock(RoleRepository.class);
        assignmentRepository = mock(TenantAdminAssignmentRepository.class);
        permissionGrantRepository = mock(TenantUserPermissionGrantRepository.class);
        messageService = mock(LocalizedMessageService.class);
        memberEffectivePermissionService = mock(MemberEffectivePermissionService.class);

        service = new CustomUserDetailService(
                userRepository,
                roleRepository,
                assignmentRepository,
                permissionGrantRepository,
                new TenantUserAccessPolicy(),
                messageService,
                memberEffectivePermissionService
        );
    }

    @Test
    void loadUserByUsername_preservesPlatformAdminAuthorityForTenantlessUser() {
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

        when(memberEffectivePermissionService.resolvePermissions(user)).thenReturn(Set.of());
        when(userRepository.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));

        UserPrincipal principal = (UserPrincipal) service.loadUserByUsername(user.getEmail());

        assertThat(principal.getRoleNames()).contains("PLATFORM_ADMIN");
        assertThat(principal.getAuthorities())
                .extracting(authority -> authority.getAuthority())
                .contains("ROLE_PLATFORM_ADMIN");
    }

    @Test
    void loadUserByUsername_fallsBackWhenTenantAdminAssignmentLookupFails() {
        UUID tenantId = UUID.randomUUID();
        Role userRole = Role.builder()
                .id(11L)
                .roleName("USER")
                .build();

        UserEntity user = UserEntity.builder()
                .uuid(UUID.randomUUID())
                .email("tenant-user@example.com")
                .password("secret")
                .fullName("Tenant User")
                .roles(Set.of(userRole))
                .build();
        user.setTenantId(tenantId);

        when(userRepository.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
        when(assignmentRepository.findByTenant_IdAndUserId(tenantId, user.getUuid()))
                .thenThrow(new InvalidDataAccessResourceUsageException("tenant_admin_assignments missing"));
        when(permissionGrantRepository.findByUserIdAndTenantId(user.getUuid(), tenantId)).thenReturn(List.of());
        when(memberEffectivePermissionService.resolvePermissions(user)).thenReturn(Set.of());

        UserPrincipal principal = (UserPrincipal) service.loadUserByUsername(user.getEmail());

        assertThat(principal.getRoleNames()).contains("USER");
    }

    @Test
    void loadUserByUsername_skipsDirectPermissionEnrichmentWhenGrantLookupFails() {
        UUID tenantId = UUID.randomUUID();
        Role userRole = Role.builder()
                .id(12L)
                .roleName("USER")
                .build();

        UserEntity user = UserEntity.builder()
                .uuid(UUID.randomUUID())
                .email("grant-user@example.com")
                .password("secret")
                .fullName("Grant User")
                .roles(Set.of(userRole))
                .build();
        user.setTenantId(tenantId);

        when(userRepository.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
        when(assignmentRepository.findByTenant_IdAndUserId(tenantId, user.getUuid())).thenReturn(Optional.empty());
        when(memberEffectivePermissionService.resolvePermissions(user)).thenReturn(Set.of());
        when(permissionGrantRepository.findByUserIdAndTenantId(user.getUuid(), tenantId))
                .thenThrow(new InvalidDataAccessResourceUsageException("tenant_user_permission_grants missing"));

        UserPrincipal principal = (UserPrincipal) service.loadUserByUsername(user.getEmail());

        assertThat(principal.getRoleNames()).contains("USER");
        assertThat(principal.getAuthorities())
                .extracting(authority -> authority.getAuthority())
                .contains("ROLE_USER");
    }
}
