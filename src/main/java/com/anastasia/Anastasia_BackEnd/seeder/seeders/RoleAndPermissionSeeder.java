package com.anastasia.Anastasia_BackEnd.seeder.seeders;

import com.anastasia.Anastasia_BackEnd.model.permission.Permission;
import com.anastasia.Anastasia_BackEnd.model.permission.PermissionType;
import com.anastasia.Anastasia_BackEnd.model.role.Role;
import com.anastasia.Anastasia_BackEnd.model.role.RoleType;
import com.anastasia.Anastasia_BackEnd.repository.auth.PermissionRepository;
import com.anastasia.Anastasia_BackEnd.repository.auth.RoleRepository;
import com.anastasia.Anastasia_BackEnd.seeder.DataSeeder;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
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
                    //System.out.println("Permission " + perm.name() + " saved successfully.");
                }
            }
        } catch (Exception e) {
            logger.error("Error during data seeding: {}", e.getMessage(), e);
            // Optionally, rethrow the exception if you want the entire seeding process to stop
            // throw new RuntimeException("Error occurred while seeding permissions.", e);
        }
    }

    public void seedDefaultRoles() {
        if (!roleRepository.existsByRoleName("USER")) {
            createRole(RoleType.USER);
            System.out.println("USER role created");
        }

        if (!roleRepository.existsByRoleName("OWNER")) {
            createRole(RoleType.OWNER);
            System.out.println("OWNER role created");
        }

        if (!roleRepository.existsByRoleName("ADMIN")) {
            createRole(RoleType.ADMIN);
            System.out.println("ADMIN role created");

        }  if (!roleRepository.existsByRoleName("PRIEST")) {
            createRole(RoleType.PRIEST);
            System.out.println("PRIEST role created");
        }


    }

    private void createRole(RoleType roleType) {

        if(!roleRepository.existsByRoleName(roleType.name())) {


            Set<String> permissionNames = roleType.getPermissions().stream()
                    .map(PermissionType::name) // Converts ENUM to String
                    .collect(Collectors.toSet());

            Set<Permission> permissions = permissionRepository.findByNameIn(permissionNames);

            logger.info("Permissions {}", permissions);

            Role role = Role.builder()
                    .roleName(roleType.name())
                    .description(roleType.getDescription())
                    .permissions(permissions)
                    .tenant(null) // Default roles are global
                    .build();

            roleRepository.save(role);
        }
    }
}
