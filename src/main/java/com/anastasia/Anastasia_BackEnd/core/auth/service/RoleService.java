package com.anastasia.Anastasia_BackEnd.core.auth.service;

import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.common.i18n.LocalizedMessageService;
import com.anastasia.Anastasia_BackEnd.core.auth.permission.Permission;
import com.anastasia.Anastasia_BackEnd.core.auth.permission.PermissionType;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.PermissionRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.RoleRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.role.Role;
import com.anastasia.Anastasia_BackEnd.core.auth.role.RoleRequest;
import com.anastasia.Anastasia_BackEnd.core.auth.role.RoleResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleService {

    private static final Set<String> SYSTEM_ROLE_NAMES = Set.of(
            "OWNER",
            "ADMIN",
            "PRIEST",
            "MEMBER",
            "USER",
            "CHILD",
            "GROUP_LEADER",
            "PLATFORM_ADMIN"
    );

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final LocalizedMessageService messageService;

    @Transactional(readOnly = true)
    public List<RoleResponse> listRoles() {
        UUID tenantId = requireTenantId();
        return roleRepository.findSystemAndTenantRoles(tenantId).stream()
                .map(this::toRoleResponse)
                .toList();
    }

    @Transactional
    public RoleResponse createRole(RoleRequest roleRequest) {
        UUID tenantId = requireTenantId();
        String normalizedRoleName = normalizeRoleName(roleRequest.getRoleName());

        if (isSystemRoleName(normalizedRoleName)) {
            throw new IllegalArgumentException(messageService.get("role.system.createForbidden", "System roles cannot be created or modified"));
        }

        if (roleRepository.existsByRoleNameAndTenantId(normalizedRoleName, tenantId)) {
            throw new IllegalArgumentException(messageService.get("role.alreadyExists", "Role already exists for this tenant"));
        }

        TenantEntity tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new EntityNotFoundException("No valid Tenant found"));

        Set<Permission> permissions = resolvePermissions(roleRequest.getPermissions());

        Role role = Role.builder()
                .roleName(normalizedRoleName)
                .description(roleRequest.getDescription())
                .permissions(permissions)
                .tenant(tenant)
                .build();

        Role saved = roleRepository.save(role);
        return toRoleResponse(saved);
    }

    @Transactional
    public RoleResponse updateRole(Long roleId, RoleRequest roleRequest) {
        UUID tenantId = requireTenantId();
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new EntityNotFoundException("Role not found"));

        if (isSystemRole(role)) {
            throw new IllegalArgumentException(messageService.get("role.system.editForbidden", "System roles are not editable"));
        }

        if (!tenantId.equals(role.getTenantId())) {
            throw new EntityNotFoundException(messageService.get("role.notFoundInTenant", "Role not found in tenant"));
        }

        String normalizedRoleName = normalizeRoleName(roleRequest.getRoleName());
        if (!normalizedRoleName.equals(role.getRoleName())
                && roleRepository.existsByRoleNameAndTenantId(normalizedRoleName, tenantId)) {
            throw new IllegalArgumentException(messageService.get("role.nameAlreadyExists", "Role name already exists for this tenant"));
        }

        if (isSystemRoleName(normalizedRoleName)) {
            throw new IllegalArgumentException(messageService.get("role.system.nameReserved", "System role names are reserved"));
        }

        role.setRoleName(normalizedRoleName);
        role.setDescription(roleRequest.getDescription());
        role.setPermissions(resolvePermissions(roleRequest.getPermissions()));

        Role saved = roleRepository.save(role);
        return toRoleResponse(saved);
    }

    @Transactional
    public void deleteRole(Long roleId) {
        UUID tenantId = requireTenantId();
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new EntityNotFoundException("Role not found"));

        if (isSystemRole(role)) {
            throw new IllegalArgumentException(messageService.get("role.system.deleteForbidden", "System roles cannot be deleted"));
        }

        if (!tenantId.equals(role.getTenantId())) {
            throw new EntityNotFoundException(messageService.get("role.notFoundInTenant", "Role not found in tenant"));
        }

        roleRepository.delete(role);
    }

    private Set<Permission> resolvePermissions(Set<PermissionType> permissionTypes) {
        if (permissionTypes == null || permissionTypes.isEmpty()) {
            return Set.of();
        }

        Set<String> enumNames = permissionTypes.stream()
                .map(Enum::name)
                .collect(Collectors.toSet());

        return permissionRepository.findByNameIn(enumNames);
    }

    private RoleResponse toRoleResponse(Role role) {
        List<String> permissionKeys = role.getPermissions().stream()
                .map(permission -> permission.getName().getName())
                .sorted()
                .toList();

        return RoleResponse.builder()
                .id(role.getId())
                .roleName(role.getRoleName())
                .description(role.getDescription())
                .system(isSystemRole(role))
                .tenantId(role.getTenantId())
                .permissions(permissionKeys)
                .userCount(userRepository.countByRoles_Id(role.getId()))
                .build();
    }

    private boolean isSystemRole(Role role) {
        return role.getTenantId() == null || isSystemRoleName(role.getRoleName());
    }

    private boolean isSystemRoleName(String roleName) {
        return SYSTEM_ROLE_NAMES.contains(roleName);
    }

    private String normalizeRoleName(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            throw new IllegalArgumentException(messageService.get("validation.role.name.required", "Role name is required"));
        }
        return roleName.trim().toUpperCase(Locale.ROOT);
    }

    private UUID requireTenantId() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException(messageService.get("tenant.context.missing", "Tenant ID is not set in the context"));
        }
        return tenantId;
    }
}
