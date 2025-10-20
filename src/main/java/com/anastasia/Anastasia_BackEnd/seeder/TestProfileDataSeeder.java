package com.anastasia.Anastasia_BackEnd.seeder;

import com.anastasia.Anastasia_BackEnd.model.permission.Permission;
import com.anastasia.Anastasia_BackEnd.model.permission.PermissionType;
import com.anastasia.Anastasia_BackEnd.model.role.Role;
import com.anastasia.Anastasia_BackEnd.model.role.RoleType;
import com.anastasia.Anastasia_BackEnd.repository.auth.PermissionRepository;
import com.anastasia.Anastasia_BackEnd.repository.auth.RoleRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Minimal data seeder that runs when the application is started with the
 * {@code test} or {@code test-server} profile. It ensures that permissions and
 * default roles required by the API automation suite exist before tests start.
 */
@Component
@Profile({"test", "test-server"})
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
        Arrays.stream(RoleType.values()).forEach(this::createRoleIfMissing);
    }

    private void createRoleIfMissing(RoleType roleType) {
        if (roleRepository.existsByRoleName(roleType.name())) {
            return;
        }

        Set<String> permissionNames = roleType.getPermissions().stream()
                .map(PermissionType::name)
                .collect(Collectors.toSet());

        Set<Permission> permissions = permissionNames.isEmpty()
                ? Set.of()
                : permissionRepository.findByNameIn(permissionNames);

        Role role = Role.builder()
                .roleName(roleType.name())
                .description(roleType.getDescription())
                .permissions(permissions)
                .build();

        roleRepository.save(role);
        log.debug("Created {} role for test profile", roleType.name());
    }
}
