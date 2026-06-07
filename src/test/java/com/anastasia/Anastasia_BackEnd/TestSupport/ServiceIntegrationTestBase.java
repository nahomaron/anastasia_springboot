package com.anastasia.Anastasia_BackEnd.TestSupport;

import com.anastasia.Anastasia_BackEnd.TestDataUtil;
import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.PermissionRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.support.TestActivationTokenStore;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.core.auth.permission.PermissionType;
import com.anastasia.Anastasia_BackEnd.core.auth.role.Role;
import com.anastasia.Anastasia_BackEnd.core.auth.role.RoleType;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.core.auth.principal.UserPrincipal;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChurchRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.RoleRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.MemberRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.ChurchService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Shared base class for service-level integration tests.
 * Boots the full Spring context with an in-memory database and
 * exposes helper methods for multi-tenant and security-aware services.
 */
@ActiveProfiles("test")
@SpringBootTest
public abstract class ServiceIntegrationTestBase {

    @Autowired protected TenantRepository tenantRepository;
    @Autowired protected ChurchRepository churchRepository;
    @Autowired protected RoleRepository roleRepository;
    @Autowired protected PermissionRepository permissionRepository;
    @Autowired protected UserRepository userRepository;
    @Autowired protected PasswordEncoder passwordEncoder;
    @Autowired protected ChurchService churchService;
    @Autowired protected MemberRepository memberRepository;
    @Autowired protected TestActivationTokenStore activationTokenStore;

    protected TenantEntity tenant;
    protected ChurchEntity church;

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:anastasia;MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
        registry.add("spring.datasource.driverClassName", () -> "org.h2.Driver");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.H2Dialect");
    }

    @BeforeEach
    void baseSetup() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
        tenant = tenantRepository.save(TestDataUtil.createTestTenantEntity());
        TenantContext.setTenantId(tenant.getId());

        String churchNumber = churchService.createChurch(TestDataUtil.createTestChurchEntity(tenant)).getChurchNumber();
        church = churchRepository.findByChurchNumber(churchNumber)
                .orElseThrow(() -> new IllegalStateException("Failed to persist default church"));
    }

    @AfterEach
    void clearContexts() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    protected Role fetchRole(RoleType roleType) {
        return roleRepository.findByRoleName(roleType.name())
                .orElseThrow(() -> new IllegalStateException("Role not seeded: " + roleType));
    }

    protected Role createAdHocRole(String suffix, Set<PermissionType> permissions) {
        Set<String> names = permissions.stream()
                .map(PermissionType::name)
                .collect(Collectors.toSet());

        var permissionEntities = permissionRepository.findByNameIn(names);

        return roleRepository.save(Role.builder()
                .roleName("IT_ROLE_" + suffix + "_" + UUID.randomUUID())
                .description("Integration test role")
                .tenant(tenant)
                .permissions(permissionEntities)
                .build());
    }

    protected UserEntity persistUser(String email, Role role) {
        UserEntity user = UserEntity.builder()
                .fullName("Integration Tester")
                .email(email)
                .password(passwordEncoder.encode(TestDataUtil.TEST_PASSWORD))
                .emailVerifiedAt(Instant.now())
                .roles(new HashSet<>(Set.of(role)))
                .affiliatedTenant(tenant)
                .build();
        return userRepository.save(user);
    }

    protected void authenticate(UserEntity user) {
        UserPrincipal principal = new UserPrincipal(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                principal,
                user.getPassword(),
                principal.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    protected String rawActivationToken(String email) {
        return activationTokenStore.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("No raw activation token captured for " + email));
    }
}
