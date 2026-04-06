package com.anastasia.Anastasia_BackEnd.Api.utils;

import com.anastasia.Anastasia_BackEnd.Api.base.BaseApiTest;
import com.anastasia.Anastasia_BackEnd.Api.flows.AuthFlowHelper;
import com.anastasia.Anastasia_BackEnd.Api.flows.PlatformAdminFlowHelper;
import com.anastasia.Anastasia_BackEnd.Api.flows.SubscriptionFlowHelper;
import com.anastasia.Anastasia_BackEnd.Api.services.AuthService;
import com.anastasia.Anastasia_BackEnd.Api.utils.DataGenerator;
import com.anastasia.Anastasia_BackEnd.core.auth.dto.AuthenticationRequest;
import com.anastasia.Anastasia_BackEnd.core.auth.dto.AuthenticationResponse;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RoleContextFactory:
 * Dynamically provisions users for each role (OWNER, ADMIN, PRIEST, MEMBER, USER)
 * using legitimate business flows. Fully black-box: interacts only with REST APIs.
 */
public final class RoleContextFactory {

    private static final Logger log = LoggerFactory.getLogger(RoleContextFactory.class);
    private static final Map<String, RequestSpecification> specCache = new ConcurrentHashMap<>();
    private static final AuthService authService = new AuthService();

    private RoleContextFactory() {}

    /** Entry point for provisioned RestAssured specifications by role. */
    public static RequestSpecification getSpecForRole(String roleName) {
        String normalizedRole = roleName.toUpperCase();
        if ("OWNER".equals(normalizedRole)) {
            return buildOwnerSpec();
        }
        return specCache.computeIfAbsent(normalizedRole, RoleContextFactory::createSpecForRole);
    }

    private static RequestSpecification buildOwnerSpec() {
        BaseApiTest.ensureOwnerAuthenticated();
        String token = BaseApiTest.getOwnerAccessToken();
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("Owner token is not initialized");
        }
        UUID tenantId = BaseApiTest.getCachedTenantId();
        return buildSpec("OWNER", token, tenantId);
    }

    private static RequestSpecification createSpecForRole(String roleName) {
        log.info("🚀 Initializing RequestSpecification for role: {}", roleName);

        return switch (roleName.toUpperCase()) {
            case "ADMIN" -> buildSpecForProvisionedUser(roleName, Set.of(requireRole("ADMIN")));
            case "PLATFORM_ADMIN" -> {
                AuthenticationResponse admin = PlatformAdminFlowHelper.registerAndLogin(
                        DataGenerator.randomEmail(),
                        DataGenerator.randomPassword());
                UUID tenantId = admin.getSession() != null ? admin.getSession().getTenantId() : null;
                yield buildSpec(roleName, admin.getAccessToken(), tenantId);
            }
            case "PRIEST" -> buildSpecForProvisionedUser(roleName, Set.of(requireRole("PRIEST")));
            case "MEMBER" -> {
                Set<Long> extras = new HashSet<>();
                try {
                    extras.add(requireRole("MEMBER"));
                } catch (IllegalStateException ex) {
                    log.warn("MEMBER role not found; falling back to USER privileges only.");
                }
                yield buildSpecForProvisionedUser(roleName, extras);
            }
            case "USER" -> buildSpecForProvisionedUser(roleName, Collections.emptySet());
            case "GUEST" -> new RequestSpecBuilder()
                    .setContentType(ContentType.JSON)
                    .build();
            default -> throw new IllegalArgumentException("Unknown role: " + roleName);
        };
    }

    private static RequestSpecification buildSpecForProvisionedUser(String roleName, Set<Long> additionalRoleIds) {
        String email = DataGenerator.randomEmail();
        String password = DataGenerator.randomPassword();

        AuthFlowHelper.signUpAndActivateAndLogin(email, password);

        assignRolesIncludingUser(email, additionalRoleIds);

        AuthenticationResponse authRes = authService.loginAndExtractToken(new AuthenticationRequest(email, password));
        if (authRes == null || authRes.getAccessToken() == null) {
            throw new IllegalStateException("Failed to obtain access token for role " + roleName);
        }
        UUID tenantId = authRes.getSession() != null ? authRes.getSession().getTenantId() : null;

        return buildSpec(roleName, authRes.getAccessToken(), tenantId);
    }

    private static void assignRolesIncludingUser(String email, Set<Long> additionalRoleIds) {
        Set<Long> roles = new HashSet<>();
        roles.add(requireRole("USER"));
        if (additionalRoleIds != null) {
            roles.addAll(additionalRoleIds);
        }
        RoleSeeder.assignRolesToUser(ownerToken(), email, roles);
    }

    private static Long requireRole(String roleName) {
        return RoleIdResolver.getRoleId(roleName);
    }

    static String ownerToken() {
        BaseApiTest.ensureOwnerAuthenticated();
        String token = BaseApiTest.getOwnerAccessToken();
        if (token == null || token.isBlank()) {
            token = SubscriptionFlowHelper.subscribeTenantAndLoginOwner().accessToken();
        }
        return token;
    }

    private static RequestSpecification buildSpec(String role, String token, UUID tenantId) {
        log.info("✅ Cached token for role {} (len: {})", role, token != null ? token.length() : 0);
        RequestSpecBuilder builder = new RequestSpecBuilder()
                .setContentType(ContentType.JSON)
                .addHeader("Authorization", "Bearer " + token);

        UUID tenantIdHeader = tenantId != null ? tenantId : BaseApiTest.getCachedTenantId();
        if (tenantIdHeader != null) {
            builder.addHeader("X-Tenant-ID", tenantIdHeader.toString());
        }

        return builder.build();
    }
}
