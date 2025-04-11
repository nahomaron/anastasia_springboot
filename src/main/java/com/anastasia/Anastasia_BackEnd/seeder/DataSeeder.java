package com.anastasia.Anastasia_BackEnd.seeder;

import com.anastasia.Anastasia_BackEnd.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.model.user.UserEntity;
import com.anastasia.Anastasia_BackEnd.seeder.seeders.*;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
public class DataSeeder {

    private static final Logger logger = LoggerFactory.getLogger(DataSeeder.class);  // Use SLF4J logger

    private final MemberSeeder memberSeeder;
    private final RoleAndPermissionSeeder roleAndPermissionSeeder;
    private final UserSeeder userSeeder;
    private final ChurchSeeder churchSeeder;
    private final PriestSeeder priestSeeder;
    private final TenantSeeder tenantSeeder;

    @PostConstruct
    @Transactional
    public void init() {
        try {
            logger.info("starting database seeding ...");
            roleAndPermissionSeeder.seedPermissions();
            roleAndPermissionSeeder.seedDefaultRoles();

            List<UserEntity> savedUsers = userSeeder.seedUsers();
            List<TenantEntity> savedTenants = tenantSeeder.seedTenants();
            List<ChurchEntity> savedChurches = churchSeeder.seedChurches(savedTenants);
            priestSeeder.seedPriests(savedChurches);
            memberSeeder.seedMembers(savedChurches);

            logger.info("Data seeding completed successfully.");
        } catch (Exception e) {
            logger.error("Error during data seeding: {}", e.getMessage(), e);
        }
    }

}


