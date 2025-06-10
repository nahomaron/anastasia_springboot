package com.anastasia.Anastasia_BackEnd.seeder;

import com.anastasia.Anastasia_BackEnd.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.model.role.Role;
import com.anastasia.Anastasia_BackEnd.model.tenant.SubscriptionPlan;
import com.anastasia.Anastasia_BackEnd.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.model.tenant.TenantType;
import com.anastasia.Anastasia_BackEnd.model.user.UserEntity;
import com.anastasia.Anastasia_BackEnd.model.user.UserType;
import com.anastasia.Anastasia_BackEnd.repository.ChurchRepository;
import com.anastasia.Anastasia_BackEnd.repository.auth.RoleRepository;
import com.anastasia.Anastasia_BackEnd.repository.auth.UserRepository;
import com.anastasia.Anastasia_BackEnd.repository.TenantRepository;
import com.anastasia.Anastasia_BackEnd.service.group.GroupServiceImpl;
import com.anastasia.Anastasia_BackEnd.service.registration.ChurchService;
import jakarta.persistence.EntityNotFoundException;
import org.h2.engine.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@TestConfiguration
public class TestDataSeederConfig {

    private static final Logger logger = LoggerFactory.getLogger(TestDataSeederConfig.class);  // Use SLF4J logger

    public static final String TEST_EMAIL = "weldit@gmail.com";
    public static final String TEST_PASSWORD = "WGebray@123";
    public static UUID TEST_USER_UUID;

    @Bean
    public boolean seedTestData(
            UserRepository userRepository,
            TenantRepository tenantRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            ChurchRepository churchRepository,
            ChurchService churchService // ✅ Inject your service
    ) {
        // 1. Create and save tenant
        TenantEntity tenant = TenantEntity.builder()
                .tenantType(TenantType.CHURCH)
                .ownerName("Test Church")
                .phoneNumber("123456789")
                .subscriptionPlan(SubscriptionPlan.BASIC)
                .isActiveTenant(true)
                .isPaymentConfirmed(true)
                .build();

        TenantEntity savedTenant = tenantRepository.save(tenant);

        // 2. Set TenantContext manually (required by ChurchService)
        TenantContext.setTenantId(savedTenant.getId());

        // 3. Build and create church using service (to trigger full logic)
        ChurchEntity church = ChurchEntity.builder()
//                .churchId(11L)
                .churchNumber("M1234")
                .tenant(savedTenant)
                .churchName("Test Church")
                .diocese("Test Diocese")
                .build();


        ChurchEntity savedChurch = churchRepository.save(church);

        savedTenant.assignChurch(savedChurch);
        tenantRepository.save(savedTenant);

        // 4. Save owner role and user
        Role ownerRole = roleRepository.findByRoleName("OWNER")
                .orElseThrow(() -> new EntityNotFoundException("No role found"));

        UserEntity user = UserEntity.builder()
                .email("weldit@gmail.com")
                .password(passwordEncoder.encode("WGebray@123"))
                .fullName("Test User")
                .verified(true)
                .roles(Set.of(ownerRole))
                .userType(UserType.GUEST)
                .tenant(savedTenant)
                .tenantId(savedTenant.getId())
                .build();

        UserEntity savedUser = userRepository.save(user);
        TEST_USER_UUID = savedUser.getUuid();

        // 5. Clear tenant context
        TenantContext.clear();

        return true;
    }


}

