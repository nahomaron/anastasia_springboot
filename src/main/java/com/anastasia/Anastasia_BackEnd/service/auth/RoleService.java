package com.anastasia.Anastasia_BackEnd.service.auth;

import com.anastasia.Anastasia_BackEnd.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.model.permission.Permission;
import com.anastasia.Anastasia_BackEnd.model.permission.PermissionType;
import com.anastasia.Anastasia_BackEnd.model.role.Role;
import com.anastasia.Anastasia_BackEnd.model.role.RoleRequest;
import com.anastasia.Anastasia_BackEnd.repository.TenantRepository;
import com.anastasia.Anastasia_BackEnd.repository.auth.PermissionRepository;
import com.anastasia.Anastasia_BackEnd.repository.auth.RoleRepository;
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
