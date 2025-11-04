package com.anastasia.Anastasia_BackEnd.Api.utils;

import com.anastasia.Anastasia_BackEnd.TestDataUtil;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.Adult_MemberDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.Adult_MemberEntity;
import com.anastasia.Anastasia_BackEnd.core.auth.role.Role;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChurchRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.RoleRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.MemberRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Component
public class TestDataSeeder {

    public static final String ADMIN_EMAIL = "admin@test.com";
    public static final String ADMIN_PASSWORD = TestDataUtil.TEST_PASSWORD;

    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final MemberRepository memberRepo;
    private final TenantRepository tenantRepo; // New dependency for TenantEntity
    private final ChurchRepository churchRepo; // New dependency for ChurchEntity
    private final PasswordEncoder passwordEncoder;

    public TestDataSeeder(
            UserRepository userRepo,
            RoleRepository roleRepo,
            MemberRepository memberRepo,
            TenantRepository tenantRepo,
            ChurchRepository churchRepo,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepo = userRepo;
        this.roleRepo = roleRepo;
        this.memberRepo = memberRepo;
        this.tenantRepo = tenantRepo;
        this.churchRepo = churchRepo;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Creates a test tenant, an 'ADMIN' role linked to the tenant, and a User
     * assigned that role.
     * @return The created UserEntity.
     */
    @Transactional
    public UserEntity createAdminUser() {
        return userRepo.findByEmail(ADMIN_EMAIL).orElseGet(() -> {
            TenantEntity tenant = tenantRepo.save(TestDataUtil.createTestTenantEntity());
            ChurchEntity church = churchRepo.save(TestDataUtil.createTestChurchEntity(tenant));
            tenant.assignChurch(church);
            tenantRepo.save(tenant);

            Role adminRole = roleRepo.findByRoleName("ADMIN")
                    .orElseThrow(() -> new IllegalStateException("Admin role not seeded"));

            UserEntity user = TestDataUtil.createTestUserEntityA();
            user.setEmail(ADMIN_EMAIL);
            user.setPassword(passwordEncoder.encode(ADMIN_PASSWORD));
            user.setRoles(Set.of(adminRole));
            user.setVerified(true);
            user.assignTenant(tenant);

            return userRepo.save(user);
        });
    }

    /**
     * Ensures a Church exists for the seeded tenant and returns it so tests can reference the church context.
     */
    @Transactional
    public ChurchEntity ensureChurchForSeededTenant() {
        return churchRepo.findAll().stream()
                .findFirst()
                .orElseGet(() -> {
                    TenantEntity tenant = tenantRepo.save(TestDataUtil.createTestTenantEntity());
                    ChurchEntity church = churchRepo.save(TestDataUtil.createTestChurchEntity(tenant));
                    tenant.assignChurch(church);
                    tenantRepo.save(tenant);
                    return church;
                });
    }

    /**
     * Creates or reuses a tenant scoped member attached to the available church.
     * @param firstName The desired first name for the member.
     * @return The created MemberEntity.
     */
    @Transactional
    public Adult_MemberEntity createMember(String firstName) {
        ChurchEntity church = ensureChurchForSeededTenant();

        Adult_MemberEntity member = TestDataUtil.createTestMember(church);
        member.setFirstName(firstName);
        member.setChurchNumber(church.getChurchNumber());

        return memberRepo.save(member);
    }

    /**
     * Factory-style builder for MemberDTO (for RestAssured or MockMvc).
     * This uses the utility method for full population.
     */
    public Adult_MemberDTO createMemberDTO(ChurchEntity church) {
        return TestDataUtil.createTestMemberDTO(church);
    }
}
