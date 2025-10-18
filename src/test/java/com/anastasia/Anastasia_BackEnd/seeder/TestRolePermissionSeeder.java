package com.anastasia.Anastasia_BackEnd.seeder;

import com.anastasia.Anastasia_BackEnd.model.permission.Permission;
import com.anastasia.Anastasia_BackEnd.model.permission.PermissionType;
import com.anastasia.Anastasia_BackEnd.model.role.Role;
import com.anastasia.Anastasia_BackEnd.model.role.RoleType;
import com.anastasia.Anastasia_BackEnd.repository.auth.PermissionRepository;
import com.anastasia.Anastasia_BackEnd.repository.auth.RoleRepository;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

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
            if (roleRepository.existsByRoleName(roleType.name())) {
                continue;
            }

            Set<String> permissionNames = roleType.getPermissions().stream()
                    .map(PermissionType::name)
                    .collect(Collectors.toSet());

            Set<Permission> permissions = permissionRepository.findByNameIn(permissionNames);

            Role role = Role.builder()
                    .roleName(roleType.name())
                    .description(roleType.getDescription())
                    .permissions(permissions)
                    .tenant(null)
                    .build();

            roleRepository.save(role);
            log.debug("Seeding role {}", roleType.name());
        }
    }
}
