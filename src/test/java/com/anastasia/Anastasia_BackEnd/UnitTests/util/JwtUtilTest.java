package com.anastasia.Anastasia_BackEnd.UnitTests.util;

import com.anastasia.Anastasia_BackEnd.core.auth.permission.Permission;
import com.anastasia.Anastasia_BackEnd.core.auth.permission.PermissionType;
import com.anastasia.Anastasia_BackEnd.core.auth.principal.UserPrincipal;
import com.anastasia.Anastasia_BackEnd.core.auth.role.Role;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.BillingProvider;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.SubscriptionPlan;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.SubscriptionStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantSubscriptionEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantType;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.common.utils.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilTest {
    private JwtUtil jwtUtil;
    private UserPrincipal userPrincipal;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(com.anastasia.Anastasia_BackEnd.util.TestJwtSecrets.currentSecret());

        TenantEntity tenant = TenantEntity.builder()
                .id(UUID.randomUUID())
                .tenantType(TenantType.CHURCH)
                .ownerName("Owner")
                .phoneNumber("+251900000000")
                .build();
        tenant.assignSubscription(
                TenantSubscriptionEntity.builder()
                        .plan(SubscriptionPlan.BASIC)
                        .status(SubscriptionStatus.ACTIVE)
                        .provider(BillingProvider.MANUAL)
                        .build()
        );

        Role role = Role.builder()
                .roleName("ADMIN")
                .permissions(Set.of(Permission.builder().name(PermissionType.MANAGE_USERS).build()))
                .build();

        UserEntity userEntity = UserEntity.builder()
                .uuid(UUID.randomUUID())
                .email("test@example.com")
                .password("Password1!")
                .roles(Set.of(role))
                .affiliatedTenant(tenant)
                .build();

        userPrincipal = new UserPrincipal(userEntity);
    }

    @Test
    void generateClaims_shouldIncludeTenantAndRoles() {
        Map<String, Object> claims = jwtUtil.generateClaims(userPrincipal);

        assertThat(claims.get("tenantId")).isEqualTo(userPrincipal.getTenantId().toString());
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) claims.get("roles");
        assertThat(roles).contains("ROLE_ADMIN");
    }

    @Test
    void generateClaims_whenNotUserPrincipal_shouldThrow() {
        UserDetails other = User.withUsername("user").password("pass").authorities("ROLE_USER").build();

        assertThatThrownBy(() -> jwtUtil.generateClaims(other))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void accessToken_shouldContainUsernameAndBeValid() {
        String token = jwtUtil.generateAccessToken(userPrincipal);

        assertThat(jwtUtil.extractUsername(token)).isEqualTo(userPrincipal.getUsername());
        assertThat(jwtUtil.isTokenValid(token, userPrincipal)).isTrue();
        assertThat(jwtUtil.extractRoles(token)).contains("ROLE_ADMIN");
        assertThat(jwtUtil.extractTenantId(token)).isEqualTo(userPrincipal.getTenantId().toString());
    }

    @Test
    void isTokenExpired_shouldReturnTrueForExpiredToken() {
        String token = jwtUtil.buildToken(Map.of(), userPrincipal, -1000L);

        assertThat(jwtUtil.isTokenExpired(token)).isTrue();
    }

    @Test
    void extractAllClaims_shouldAcceptTokenSignedWithPreviousSecret() {
        String currentSecret = com.anastasia.Anastasia_BackEnd.util.TestJwtSecrets.currentSecret();
        String previousSecret = com.anastasia.Anastasia_BackEnd.util.TestJwtSecrets.previousSecret();
        JwtUtil previousSigner = new JwtUtil(previousSecret);
        JwtUtil rotatingVerifier = JwtUtil.forSecrets(currentSecret, previousSecret);

        String token = previousSigner.generateAccessToken(userPrincipal);

        assertThat(rotatingVerifier.extractUsername(token)).isEqualTo(userPrincipal.getUsername());
        assertThat(rotatingVerifier.extractTenantId(token)).isEqualTo(userPrincipal.getTenantId().toString());
    }

    @Test
    void generateBase64Secret_shouldReturnUsableSecret() {
        String generated = JwtUtil.generateBase64Secret();

        assertThat(Base64.getDecoder().decode(generated)).hasSize(32);
    }
}
