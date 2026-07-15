package com.anastasia.Anastasia_BackEnd.TestSeeder;

import com.anastasia.Anastasia_BackEnd.core.auth.permission.Permission;
import com.anastasia.Anastasia_BackEnd.core.auth.permission.PermissionType;
import com.anastasia.Anastasia_BackEnd.core.auth.role.Role;
import com.anastasia.Anastasia_BackEnd.core.auth.role.RoleType;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.PermissionRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.RoleRepository;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
@Profile("test")
@RequiredArgsConstructor
public class TestRolePermissionSeeder {

    private static final Logger log = LoggerFactory.getLogger(TestRolePermissionSeeder.class);

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;

    @PostConstruct
    @Transactional
    public void seedPermissionsAndRoles() {
        seedPermissions();
        seedRoles();
    }

    private void seedPermissions() {
        for (PermissionType permissionType : PermissionType.values()) {
            permissionRepository.findByName(permissionType)
                    .orElseGet(() -> {
                        log.debug("Seeding permission {}", permissionType);
                        return permissionRepository.save(new Permission(permissionType));
                    });
        }
    }

    private void seedRoles() {
        for (RoleType roleType : RoleType.values()) {
            upsertRole(roleType);
        }
    }

    private void upsertRole(RoleType roleType) {
        Set<Permission> permissions = resolvePermissions(roleType);
        Role role = roleRepository.findByRoleName(roleType.name())
                .orElseGet(() -> Role.builder()
                        .roleName(roleType.name())
                        .tenant(null)
                        .permissions(Set.of())
                        .build());

        boolean changed = false;
        if (!roleType.getDescription().equals(role.getDescription())) {
            role.setDescription(roleType.getDescription());
            changed = true;
        }
        Set<Permission> mergedPermissions = new HashSet<>(role.getPermissions());
        if (mergedPermissions.addAll(permissions)) {
            role.setPermissions(mergedPermissions);
            changed = true;
        }
        if (role.getId() == null || changed) {
            roleRepository.save(role);
            log.debug("Synchronized role {}", roleType.name());
        }
    }

    private Set<Permission> resolvePermissions(RoleType roleType) {
        if (roleType.getPermissions().isEmpty()) {
            return Set.of();
        }
        Set<Permission> permissions = permissionRepository.findByNameIn(roleType.getPermissions());
        if (permissions.size() != roleType.getPermissions().size()) {
            throw new IllegalStateException("Missing permissions while seeding role " + roleType.name());
        }
        return permissions;
    }
}
