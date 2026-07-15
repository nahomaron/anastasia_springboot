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

@Service
@RequiredArgsConstructor
public class RoleService {

    private static final Set<String> SYSTEM_ROLE_NAMES = Set.of(
            "OWNER",
            "ADMIN",
            "PRIEST",
            "STAFF",
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
                .map(role -> toRoleResponse(role, tenantId))
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
        return toRoleResponse(saved, tenantId);
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
        return toRoleResponse(saved, tenantId);
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

        return permissionRepository.findByNameIn(permissionTypes);
    }

    private RoleResponse toRoleResponse(Role role, UUID tenantId) {
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
                .userCount(resolveUserCount(role, tenantId))
                .build();
    }

    private long resolveUserCount(Role role, UUID tenantId) {
        if (isSystemRole(role)) {
            return 0L;
        }
        return userRepository.countByRoles_IdAndAffiliatedTenantId(role.getId(), tenantId);
    }

    private boolean isSystemRole(Role role) {
        return role.getTenantId() == null || isSystemRoleName(role.getRoleName());
    }

    private boolean isSystemRoleName(String roleName) {
        return roleName != null && SYSTEM_ROLE_NAMES.contains(roleName.trim().toUpperCase(Locale.ROOT));
    }

    private String normalizeRoleName(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            throw new IllegalArgumentException(messageService.get("validation.role.name.required", "Role name is required"));
        }
        return roleName.trim();
    }

    private UUID requireTenantId() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException(messageService.get("tenant.context.missing", "Tenant ID is not set in the context"));
        }
        return tenantId;
    }
}
