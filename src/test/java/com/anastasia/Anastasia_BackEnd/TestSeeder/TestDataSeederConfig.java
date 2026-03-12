package com.anastasia.Anastasia_BackEnd.TestSeeder;

import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.core.auth.role.Role;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.BillingProvider;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.SubscriptionPlan;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.SubscriptionStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantSubscriptionEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantType;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserType;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChurchRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.RoleRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.ChurchService;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

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
                .status(TenantStatus.ACTIVE)
                .build();
        tenant.assignSubscription(
                TenantSubscriptionEntity.builder()
                        .plan(SubscriptionPlan.BASIC)
                        .status(SubscriptionStatus.ACTIVE)
                        .provider(BillingProvider.MANUAL)
                        .build()
        );

        TenantEntity savedTenant = tenantRepository.save(tenant);

        // 2. Set TenantContext manually (required by ChurchService)
        TenantContext.setTenantId(savedTenant.getId());

        // 3. Build and create church using service (to trigger full logic)
        ChurchEntity church = ChurchEntity.builder()
//                .churchId(11L)
                .churchNumber("M1234")
                .tenant(savedTenant)
                .prefix("St.")
                .tPrefix("ቅዱስ")
                .churchName("Test Church")
                .tChurchName("ቤተ ክርስቲያን ሙከራ")
                .neighborhood("Test Neighborhood")
                .tNeighborhood("መንደር ሙከራ")
                .diocese("Test Diocese")
                .email("seeded-church@example.com")
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
