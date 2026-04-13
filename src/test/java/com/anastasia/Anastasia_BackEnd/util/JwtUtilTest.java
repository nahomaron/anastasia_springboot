package com.anastasia.Anastasia_BackEnd.util;

import com.anastasia.Anastasia_BackEnd.core.auth.principal.UserPrincipal;
import com.anastasia.Anastasia_BackEnd.core.auth.role.Role;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.common.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class JwtUtilTest {
    private JwtUtil jwtUtil;
    private UserPrincipal userPrincipal;
    private final String username = "test@example.com";
    private final UUID tenantId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        TenantEntity tenant = TenantEntity.builder()
                .id(tenantId)
                .build();

        jwtUtil = new JwtUtil(TestJwtSecrets.currentSecret());

        Role role = Role.builder()
                .roleName("ADMIN")
                .build();

        UserEntity user = UserEntity.builder()
                .email("test@example.com")
                .password("password")
                .roles(Set.of(role))
                .affiliatedTenant(tenant)
                .build();

        userPrincipal = new UserPrincipal(user);

    }

    @Test
    void testGenerateAndValidateAccessToken() {
        String token = jwtUtil.generateAccessToken(userPrincipal);

        assertNotNull(token);
        assertEquals(username, jwtUtil.extractUsername(token));
        assertFalse(jwtUtil.isTokenExpired(token));
        assertTrue(jwtUtil.isTokenValid(token, userPrincipal));
    }

    @Test
    void testGenerateAndValidateRefreshToken() {
        String token = jwtUtil.generateRefreshToken(userPrincipal);

        assertNotNull(token);
        assertEquals(username, jwtUtil.extractUsername(token));
        assertFalse(jwtUtil.isTokenExpired(token));
        assertTrue(jwtUtil.isTokenValid(token, userPrincipal));
    }

    @Test
    void testExtractClaims() {
        String token = jwtUtil.generateAccessToken(userPrincipal);
        Claims claims = jwtUtil.extractAllClaims(token);

        assertEquals(username, claims.getSubject());
        assertEquals(tenantId.toString(), claims.get("tenantId"));
        List<String> roles = (List<String>) claims.get("roles");
        assertTrue(roles.contains("ROLE_ADMIN"));
    }

    @Test
    void testExtractSpecificClaims() {
        String token = jwtUtil.generateAccessToken(userPrincipal);
        String tenant = jwtUtil.extractTenantId(token);
        List<String> roles = jwtUtil.extractRoles(token);

        assertEquals(tenantId.toString(), tenant);
        assertTrue(roles.contains("ROLE_ADMIN"));
    }

    @Test
    void testTokenIsExpired() throws InterruptedException {
        String expiredToken = jwtUtil.buildToken(jwtUtil.generateClaims(userPrincipal), userPrincipal, 1); // 1 ms
        Thread.sleep(10);
        assertTrue(jwtUtil.isTokenExpired(expiredToken));
    }

    @Test
    void testValidationWithPreviousSecret() {
        JwtUtil previousSigner = new JwtUtil(TestJwtSecrets.previousSecret());
        JwtUtil rotatingVerifier = JwtUtil.forSecrets(TestJwtSecrets.currentSecret(), TestJwtSecrets.previousSecret());

        String token = previousSigner.generateAccessToken(userPrincipal);

        assertEquals(username, rotatingVerifier.extractUsername(token));
        assertEquals(tenantId.toString(), rotatingVerifier.extractTenantId(token));
    }

    @Test
    void testGenerateBase64Secret() {
        String generated = JwtUtil.generateBase64Secret();

        assertEquals(32, java.util.Base64.getDecoder().decode(generated).length);
    }
}
