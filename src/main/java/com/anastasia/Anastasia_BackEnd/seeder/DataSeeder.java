package com.anastasia.Anastasia_BackEnd.seeder;

import com.anastasia.Anastasia_BackEnd.modules.groups.model.GroupEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.seeder.seeders.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Profile("!test")
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
    private final GroupSeeder groupSeeder;
    private final EventSeeder eventSeeder;
    private final AccountingSeeder accountingSeeder;
    @Value("${app.seeding.enabled:false}")
    private boolean dataSeedingEnabled;


//    @PostConstruct
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void init() {
        logger.info("starting permissions/roles seeding ...");
        roleAndPermissionSeeder.seedPermissions();
        roleAndPermissionSeeder.seedDefaultRoles();

        if (!dataSeedingEnabled) {
            logger.info("Sample/domain data seeding is disabled (app.seeding.enabled=false).");
            return;
        }

        logger.info("starting sample/domain data seeding ...");
        userSeeder.seedUsers();
        List<TenantEntity> savedTenants = tenantSeeder.seedTenants();
        List<ChurchEntity> savedChurches = churchSeeder.seedChurches(savedTenants);
        priestSeeder.seedPriests(savedChurches);
        memberSeeder.seedMembers(savedChurches);
        List<GroupEntity> savedGroups = groupSeeder.seedGroups(savedChurches);
        eventSeeder.seedEvents(savedChurches, savedGroups);
//        accountingSeeder.seedAccounting(savedTenants);

        logger.info("Data seeding completed successfully.");
    }

}
