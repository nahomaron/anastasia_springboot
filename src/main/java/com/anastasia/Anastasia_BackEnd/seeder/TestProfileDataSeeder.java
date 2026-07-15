package com.anastasia.Anastasia_BackEnd.seeder;

import com.anastasia.Anastasia_BackEnd.core.auth.permission.Permission;
import com.anastasia.Anastasia_BackEnd.core.auth.permission.PermissionType;
import com.anastasia.Anastasia_BackEnd.core.auth.role.Role;
import com.anastasia.Anastasia_BackEnd.core.auth.role.RoleType;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.PermissionRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.RoleRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Minimal data seeder that runs when the application is started with the
 * {@code test} or {@code api-tests} profile. It ensures that permissions and
 * default roles required by the API automation suite exist before tests start.
 */
@Component
@Profile({"test", "api-tests"})
@RequiredArgsConstructor
public class TestProfileDataSeeder {

    private static final Logger log = LoggerFactory.getLogger(TestProfileDataSeeder.class);

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedTestData() {
        seedPermissions();
        seedRoles();
    }

    private void seedPermissions() {
        for (PermissionType permissionType : PermissionType.values()) {
            if (!permissionRepository.existsByName(permissionType)) {
                permissionRepository.save(new Permission(permissionType));
            }
        }
        log.debug("Ensured {} permissions exist for test profile", PermissionType.values().length);
    }

    private void seedRoles() {
        Arrays.stream(RoleType.values()).forEach(this::upsertRole);
    }

    private void upsertRole(RoleType roleType) {
        Set<Permission> permissions = resolvePermissions(roleType);
        Role role = roleRepository.findByRoleName(roleType.name())
                .orElseGet(() -> Role.builder()
                        .roleName(roleType.name())
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
            log.debug("Synchronized {} role for test profile", roleType.name());
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
