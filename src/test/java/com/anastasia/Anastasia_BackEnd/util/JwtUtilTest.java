package com.anastasia.Anastasia_BackEnd.util;

import com.anastasia.Anastasia_BackEnd.model.principal.UserPrincipal;
import com.anastasia.Anastasia_BackEnd.model.role.Role;
import com.anastasia.Anastasia_BackEnd.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.model.user.UserEntity;
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

        jwtUtil = new JwtUtil();

        Role role = Role.builder()
                .roleName("ADMIN")
                .build();

        UserEntity user = UserEntity.builder()
                .email("test@example.com")
                .password("password")
                .roles(Set.of(role))
                .tenant(tenant)
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
}
