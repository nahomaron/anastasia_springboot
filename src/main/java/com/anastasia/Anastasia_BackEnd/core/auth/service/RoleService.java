package com.anastasia.Anastasia_BackEnd.core.auth.service;

import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.core.auth.permission.Permission;
import com.anastasia.Anastasia_BackEnd.core.auth.permission.PermissionType;
import com.anastasia.Anastasia_BackEnd.core.auth.role.Role;
import com.anastasia.Anastasia_BackEnd.core.auth.role.RoleRequest;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.PermissionRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.RoleRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final TenantRepository tenantRepository;

    public void createRole(RoleRequest roleRequest) {

        UUID tenantId = TenantContext.getTenantId();

        if (roleRepository.existsByRoleNameAndTenantId(roleRequest.getRoleName(), tenantId)) {
            throw new RuntimeException("Role already exists for this tenant");
        }
        Set<String> permissionNames = roleRequest.getPermissions().stream()
                .map(PermissionType::name) // Converts ENUM to String
                .collect(Collectors.toSet());

        Set<Permission> permissions = permissionRepository.findByNameIn(permissionNames);

        Role role = Role.builder()
                .roleName(roleRequest.getRoleName())
                .description(roleRequest.getDescription())
                .permissions(permissions)
                .tenant(tenantRepository.findById(tenantId).orElseThrow(() -> new EntityNotFoundException("No valid Tenant found")))
                .build();

         roleRepository.save(role);
    }
}
