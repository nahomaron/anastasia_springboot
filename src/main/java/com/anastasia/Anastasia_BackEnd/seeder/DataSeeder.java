package com.anastasia.Anastasia_BackEnd.seeder;

import com.anastasia.Anastasia_BackEnd.model.entity.auth.Permission;
import com.anastasia.Anastasia_BackEnd.model.entity.auth.PermissionType;
import com.anastasia.Anastasia_BackEnd.model.entity.auth.Role;
import com.anastasia.Anastasia_BackEnd.repository.auth.PermissionRepository;
import com.anastasia.Anastasia_BackEnd.repository.auth.RoleRepository;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashSet;

@Component
@RequiredArgsConstructor
public class DataSeeder {

    private static final Logger logger = LoggerFactory.getLogger(DataSeeder.class);  // ✅ Use SLF4J logger

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;

    @PostConstruct
    @Transactional
    public void init() {
        try {
            // Seed permissions
            logger.info("starting database seeding ...");
            seedPermissions();
            // seed roles
            logger.info("Data seeding completed successfully.");
        } catch (Exception e) {
            logger.error("Error during data seeding: {}", e.getMessage(), e);
        }
    }

    private void seedPermissions() {
        try {
            for (PermissionType perm : PermissionType.values()) {
                // Check if permission exists in DB, and if not, save it
                if (!permissionRepository.existsByName(perm)) {
                    Permission permission = new Permission(perm);
                    permissionRepository.save(permission);
                    System.out.println("Permission " + perm.name() + " saved successfully.");
                }
            }
        } catch (Exception e) {
            logger.error("Error during data seeding: {}", e.getMessage(), e);
            // Optionally, rethrow the exception if you want the entire seeding process to stop
            // throw new RuntimeException("Error occurred while seeding permissions.", e);
        }
    }

}


