package com.anastasia.Anastasia_BackEnd.UnitTests.service.auth;

import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.core.auth.permission.Permission;
import com.anastasia.Anastasia_BackEnd.core.auth.permission.PermissionType;
import com.anastasia.Anastasia_BackEnd.core.auth.role.Role;
import com.anastasia.Anastasia_BackEnd.core.auth.role.RoleRequest;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantRepository;
import com.anastasia.Anastasia_BackEnd.common.i18n.LocalizedMessageService;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.PermissionRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.RoleRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.service.RoleService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import com.anastasia.Anastasia_BackEnd.UnitTests.support.LenientMockitoTest;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@LenientMockitoTest
public class RoleServiceTest {

    @Mock private RoleRepository roleRepository;
    @Mock private PermissionRepository permissionRepository;
    @Mock private TenantRepository tenantRepository;
    @Mock private LocalizedMessageService messageService;
    @Mock private com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository userRepository;

    @InjectMocks private RoleService roleService;

    private static final String TEST_ROLE_NAME = "Volunteer";
    private UUID tenantId;
    private RoleRequest roleRequest;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId); // manually set tenant in context

        roleRequest = new RoleRequest(TEST_ROLE_NAME, "Handles liturgical duties", Set.of(PermissionType.VIEW_MEMBERS));
        lenient().when(messageService.get(anyString(), anyString()))
                .thenAnswer(invocation -> invocation.getArgument(1));
        lenient().when(messageService.get(anyString(), anyString(), any(Object[].class)))
                .thenAnswer(invocation -> invocation.getArgument(1));
        lenient().when(userRepository.countByRoles_Id(anyLong())).thenReturn(0L);
    }

    @Test
    void testCreateRole_success() {
        // given
        Permission permission = Permission.builder().name(PermissionType.VIEW_MEMBERS).build();
        TenantEntity tenant = TenantEntity.builder().id(tenantId).build();

        when(roleRepository.existsByRoleNameAndTenantId(TEST_ROLE_NAME, tenantId)).thenReturn(false);
        when(permissionRepository.findByNameIn(Set.of("VIEW_MEMBERS"))).thenReturn(Set.of(permission));
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        assertDoesNotThrow(() -> roleService.createRole(roleRequest));

        // then
        verify(roleRepository).save(any(Role.class));
    }

    @Test
    void testCreateRole_throwsIfRoleAlreadyExists() {
        when(roleRepository.existsByRoleNameAndTenantId(TEST_ROLE_NAME, tenantId)).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                roleService.createRole(roleRequest));

        assertEquals("Role already exists for this tenant", exception.getMessage());
        verify(roleRepository, never()).save(any());
    }

    @Test
    void testCreateRole_throwsIfTenantNotFound() {
        when(roleRepository.existsByRoleNameAndTenantId(TEST_ROLE_NAME, tenantId)).thenReturn(false);
        when(permissionRepository.findByNameIn(any())).thenReturn(Set.of());
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> roleService.createRole(roleRequest));
        verify(roleRepository, never()).save(any());
    }
}
