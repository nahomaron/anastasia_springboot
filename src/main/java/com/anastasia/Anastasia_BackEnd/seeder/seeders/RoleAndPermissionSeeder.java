package com.anastasia.Anastasia_BackEnd.seeder.seeders;

import com.anastasia.Anastasia_BackEnd.core.auth.permission.Permission;
import com.anastasia.Anastasia_BackEnd.core.auth.permission.PermissionType;
import com.anastasia.Anastasia_BackEnd.core.auth.role.Role;
import com.anastasia.Anastasia_BackEnd.core.auth.role.RoleType;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.PermissionRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Component
@Profile("!test")
@RequiredArgsConstructor
public class RoleAndPermissionSeeder {
    private static final Logger logger = LoggerFactory.getLogger(RoleAndPermissionSeeder.class);  // ✅ Use SLF4J logger

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;

    public void seedPermissions() {
        try {
            for (PermissionType perm : PermissionType.values()) {
                // Check if permission exists in DB, and if not, save it
                if (!permissionRepository.existsByName(perm)) {
                    Permission permission = new Permission(perm);
                    permissionRepository.save(permission);
//                    System.out.println("Permission " + perm.name() + " saved successfully.");
                }
            }
        } catch (Exception e) {
            logger.error("Error during data seeding: {}", e.getMessage(), e);
            // Optionally, rethrow the exception if you want the entire seeding process to stop
             throw new RuntimeException("Error occurred while seeding permissions.", e);
        }
    }

    @Transactional
    public void seedDefaultRoles() {
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
            logger.debug("Synchronized role {}", roleType.name());
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
