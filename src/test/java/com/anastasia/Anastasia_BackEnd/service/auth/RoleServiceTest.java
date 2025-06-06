package com.anastasia.Anastasia_BackEnd.service.auth;

import com.anastasia.Anastasia_BackEnd.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.model.permission.Permission;
import com.anastasia.Anastasia_BackEnd.model.permission.PermissionType;
import com.anastasia.Anastasia_BackEnd.model.role.Role;
import com.anastasia.Anastasia_BackEnd.model.role.RoleRequest;
import com.anastasia.Anastasia_BackEnd.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.repository.TenantRepository;
import com.anastasia.Anastasia_BackEnd.repository.auth.PermissionRepository;
import com.anastasia.Anastasia_BackEnd.repository.auth.RoleRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RoleServiceTest {

    @Mock private RoleRepository roleRepository;
    @Mock private PermissionRepository permissionRepository;
    @Mock private TenantRepository tenantRepository;

    @InjectMocks private RoleService roleService;

    private UUID tenantId;
    private RoleRequest roleRequest;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId); // manually set tenant in context

        roleRequest = new RoleRequest("PRIEST", "Handles liturgical duties", Set.of(PermissionType.VIEW_MEMBERS));
    }

    @Test
    void testCreateRole_success() {
        // given
        Permission permission = Permission.builder().name(PermissionType.VIEW_MEMBERS).build();
        TenantEntity tenant = TenantEntity.builder().id(tenantId).build();

        when(roleRepository.existsByRoleNameAndTenantId("PRIEST", tenantId)).thenReturn(false);
        when(permissionRepository.findByNameIn(Set.of("VIEW_MEMBERS"))).thenReturn(Set.of(permission));
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));

        // when
        assertDoesNotThrow(() -> roleService.createRole(roleRequest));

        // then
        verify(roleRepository).save(any(Role.class));
    }

    @Test
    void testCreateRole_throwsIfRoleAlreadyExists() {
        when(roleRepository.existsByRoleNameAndTenantId("PRIEST", tenantId)).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                roleService.createRole(roleRequest));

        assertEquals("Role already exists for this tenant", exception.getMessage());
        verify(roleRepository, never()).save(any());
    }

    @Test
    void testCreateRole_throwsIfTenantNotFound() {
        when(roleRepository.existsByRoleNameAndTenantId("PRIEST", tenantId)).thenReturn(false);
        when(permissionRepository.findByNameIn(any())).thenReturn(Set.of());
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> roleService.createRole(roleRequest));
        verify(roleRepository, never()).save(any());
    }
}

