package com.anastasia.Anastasia_BackEnd.modules.users.service;

import com.anastasia.Anastasia_BackEnd.common.auditing.AuditEventType;
import com.anastasia.Anastasia_BackEnd.common.auditing.AuditLogService;
import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.common.i18n.LocalizedMessageService;
import com.anastasia.Anastasia_BackEnd.core.auth.permission.Permission;
import com.anastasia.Anastasia_BackEnd.core.auth.permission.PermissionType;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.PermissionRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.RoleRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.role.Role;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantRepository;
import com.anastasia.Anastasia_BackEnd.modules.users.dto.TenantAssignedRoleResponse;
import com.anastasia.Anastasia_BackEnd.modules.users.dto.TenantUserAccessResponse;
import com.anastasia.Anastasia_BackEnd.modules.users.model.TenantUserPermissionGrantEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.repository.TenantUserPermissionGrantRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TenantUserAccessService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final TenantRepository tenantRepository;
    private final TenantUserPermissionGrantRepository permissionGrantRepository;
    private final TenantUserAccessPolicy accessPolicy;
    private final LocalizedMessageService messageService;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public TenantUserAccessResponse getUserAccess(UUID userId) {
        UUID tenantId = requireTenantId();
        UserEntity user = requireTenantUser(userId, tenantId);

        Set<Role> explicitRoles = accessPolicy.explicitRolesForTenant(user, tenantId);
        Set<String> inheritedPermissions = permissionKeysFromRoles(explicitRoles);
        List<String> directPermissions = normalizedDirectPermissionKeys(userId, tenantId, inheritedPermissions);
        List<String> effectivePermissions = unionPermissions(inheritedPermissions, directPermissions);

        boolean protectedAccount = accessPolicy.isProtectedAccount(user);
        return TenantUserAccessResponse.builder()
                .userId(user.getUuid())
                .tenantId(tenantId)
                .protectedAccount(protectedAccount)
                .protectedReason(accessPolicy.protectedReason(user))
                .canEdit(!protectedAccount)
                .assignedRoles(explicitRoles.stream().map(this::toAssignedRoleResponse).toList())
                .directPermissions(directPermissions)
                .effectivePermissions(effectivePermissions)
                .build();
    }

    @Transactional
    public TenantUserAccessResponse updateUserRoles(UUID userId, Collection<Long> roleIds) {
        UUID tenantId = requireTenantId();
        UserEntity user = requireEditableTenantUser(userId, tenantId);

        List<Role> requestedRoles = roleIds == null || roleIds.isEmpty()
                ? List.of()
                : roleRepository.findAllById(roleIds);

        Set<Long> resolvedIds = requestedRoles.stream().map(Role::getId).collect(Collectors.toSet());
        Set<Long> requestedIds = roleIds == null ? Set.of() : new LinkedHashSet<>(roleIds);
        if (!resolvedIds.equals(requestedIds)) {
            throw new IllegalArgumentException(messageService.get("user.access.roles.invalid", "One or more requested roles were not found"));
        }

        List<Role> invalidRoles = requestedRoles.stream()
                .filter(role -> !accessPolicy.isAssignableThroughTenantAccess(role, tenantId))
                .toList();
        if (!invalidRoles.isEmpty()) {
            throw new IllegalArgumentException(messageService.get(
                    "user.access.roles.notAssignable",
                    "One or more requested roles cannot be assigned through tenant access"
            ));
        }

        Set<String> previousRoleNames = user.getRoles().stream()
                .map(Role::getRoleName)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        user.setRoles(new LinkedHashSet<>(requestedRoles));
        userRepository.save(user);
        auditLogService.record(
                AuditEventType.ACCESS_ROLE_CHANGED,
                "SUCCESS",
                getCurrentUserId(),
                null,
                tenantId,
                "USER",
                userId.toString(),
                null,
                "Explicit roles changed from " + previousRoleNames + " to "
                        + requestedRoles.stream().map(Role::getRoleName).toList()
        );

        return getUserAccess(userId);
    }

    @Transactional
    public TenantUserAccessResponse updateUserPermissions(UUID userId, Collection<String> permissions) {
        UUID tenantId = requireTenantId();
        UserEntity user = requireEditableTenantUser(userId, tenantId);

        Set<PermissionType> requestedPermissionTypes = normalizePermissionTypes(permissions);
        Set<Role> explicitRoles = accessPolicy.explicitRolesForTenant(user, tenantId);
        Set<String> inheritedPermissions = permissionKeysFromRoles(explicitRoles);

        Set<PermissionType> directPermissionTypesToPersist = requestedPermissionTypes.stream()
                .filter(permissionType -> !inheritedPermissions.contains(permissionType.getName()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<String> previousDirectPermissions = normalizedDirectPermissionKeys(userId, tenantId, inheritedPermissions);

        permissionGrantRepository.deleteByUserIdAndTenantId(userId, tenantId);

        if (!directPermissionTypesToPersist.isEmpty()) {
            TenantEntity tenant = tenantRepository.findById(tenantId)
                    .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));

            Set<Permission> permissionEntities = resolvePermissions(directPermissionTypesToPersist);
            UUID actorUserId = getCurrentUserId();

            List<TenantUserPermissionGrantEntity> grants = permissionEntities.stream()
                    .sorted(Comparator.comparing(permission -> permission.getName().getName()))
                    .map(permission -> TenantUserPermissionGrantEntity.builder()
                            .user(user)
                            .tenant(tenant)
                            .permission(permission)
                            .grantedByUserId(actorUserId)
                            .build())
                    .toList();

            permissionGrantRepository.saveAll(grants);
        }

        auditLogService.record(
                AuditEventType.ACCESS_PERMISSION_CHANGED,
                "SUCCESS",
                getCurrentUserId(),
                null,
                tenantId,
                "USER",
                userId.toString(),
                null,
                "Direct permissions changed from " + previousDirectPermissions + " to "
                        + directPermissionTypesToPersist.stream().map(PermissionType::getName).sorted().toList()
        );

        return getUserAccess(userId);
    }

    private UserEntity requireTenantUser(UUID userId, UUID tenantId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (user.getTenantId() == null || !tenantId.equals(user.getTenantId())) {
            throw new EntityNotFoundException(messageService.get(
                    "user.access.notFoundInTenant",
                    "User not found in current tenant"
            ));
        }

        return user;
    }

    private UserEntity requireEditableTenantUser(UUID userId, UUID tenantId) {
        UserEntity user = requireTenantUser(userId, tenantId);

        if (accessPolicy.isProtectedAccount(user)) {
            throw new IllegalArgumentException(messageService.get(
                    "user.access.protectedAccount",
                    "Protected tenant account cannot be modified"
            ));
        }

        return user;
    }

    private Set<PermissionType> normalizePermissionTypes(Collection<String> permissions) {
        Set<PermissionType> normalized = new LinkedHashSet<>();
        if (permissions == null) {
            return normalized;
        }

        for (String permission : permissions) {
            if (permission == null || permission.isBlank()) {
                continue;
            }
            normalized.add(PermissionType.fromJson(permission));
        }
        return normalized;
    }

    private Set<Permission> resolvePermissions(Set<PermissionType> permissionTypes) {
        if (permissionTypes.isEmpty()) {
            return Set.of();
        }

        Set<Permission> permissions = permissionRepository.findAllByPermissionTypes(permissionTypes);
        if (permissions.size() != permissionTypes.size()) {
            throw new IllegalArgumentException(messageService.get(
                    "user.access.permissions.invalid",
                    "One or more requested permissions were not found"
            ));
        }
        return permissions;
    }

    private Set<String> permissionKeysFromRoles(Set<Role> roles) {
        return roles.stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(permission -> permission.getName().getName())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private List<String> normalizedDirectPermissionKeys(UUID userId, UUID tenantId, Set<String> inheritedPermissions) {
        return permissionGrantRepository.findByUserIdAndTenantId(userId, tenantId).stream()
                .map(grant -> grant.getPermission().getName().getName())
                .filter(permission -> !inheritedPermissions.contains(permission))
                .distinct()
                .sorted()
                .toList();
    }

    private List<String> unionPermissions(Set<String> inheritedPermissions, List<String> directPermissions) {
        Set<String> effective = new LinkedHashSet<>(inheritedPermissions);
        effective.addAll(directPermissions);
        return effective.stream().sorted().toList();
    }

    private TenantAssignedRoleResponse toAssignedRoleResponse(Role role) {
        List<String> permissions = role.getPermissions().stream()
                .map(permission -> permission.getName().getName())
                .sorted()
                .toList();

        return TenantAssignedRoleResponse.builder()
                .id(role.getId())
                .roleName(role.getRoleName())
                .description(role.getDescription())
                .system(role.getTenantId() == null)
                .permissions(permissions)
                .build();
    }

    private UUID requireTenantId() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException(messageService.get("tenant.context.missing", "Tenant ID is not set in the context"));
        }
        return tenantId;
    }

    private UUID getCurrentUserId() {
        var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof com.anastasia.Anastasia_BackEnd.core.auth.principal.UserPrincipal userPrincipal) {
            return userPrincipal.getUserUuid();
        }
        throw new IllegalStateException(messageService.get("auth.user.notAuthenticated", "No authenticated user found."));
    }
}
