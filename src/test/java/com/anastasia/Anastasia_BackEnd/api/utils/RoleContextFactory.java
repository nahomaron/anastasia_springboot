package com.anastasia.Anastasia_BackEnd.api.utils;

import com.anastasia.Anastasia_BackEnd.api.base.BaseApiTest;
import com.anastasia.Anastasia_BackEnd.api.flows.AuthFlowHelper;
import com.anastasia.Anastasia_BackEnd.api.flows.SubscriptionFlowHelper;
import com.anastasia.Anastasia_BackEnd.api.services.AuthService;
import com.anastasia.Anastasia_BackEnd.model.auth.AuthenticationRequest;
import com.anastasia.Anastasia_BackEnd.model.auth.AuthenticationResponse;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RoleContextFactory:
 * Dynamically provisions users for each role (OWNER, ADMIN, PRIEST, MEMBER, USER)
 * using legitimate business flows. Fully black-box: interacts only with REST APIs.
 */
@Slf4j
public final class RoleContextFactory {

    private static final Map<String, RequestSpecification> specCache = new ConcurrentHashMap<>();
    private static final AuthService authService = new AuthService();

    private RoleContextFactory() {}

    /** Entry point for provisioned RestAssured specifications by role. */
    public static RequestSpecification getSpecForRole(String roleName) {
        return specCache.computeIfAbsent(roleName.toUpperCase(), RoleContextFactory::createSpecForRole);
    }

    private static RequestSpecification createSpecForRole(String roleName) {
        log.info("🚀 Initializing RequestSpecification for role: {}", roleName);

        return switch (roleName.toUpperCase()) {
            case "OWNER" -> {
                var subscription = SubscriptionFlowHelper.subscribeTenantAndLoginOwner();
                yield buildSpec(roleName, subscription.authResponse().getAccessToken());
            }
            case "ADMIN" -> buildSpecForProvisionedUser(roleName, Set.of(requireRole("ADMIN")));
            case "PLATFORM_ADMIN" ->
                    buildSpecForProvisionedUser(roleName, Set.of(requireRole("PLATFORM_ADMIN")));
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

        return buildSpec(roleName, authRes.getAccessToken());
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
        String token = BaseApiTest.getOwnerAccessToken();
        if (token == null || token.isBlank()) {
            token = SubscriptionFlowHelper.subscribeTenantAndLoginOwner().accessToken();
        }
        return token;
    }

    private static RequestSpecification buildSpec(String role, String token) {
        log.info("✅ Cached token for role {} (len: {})", role, token != null ? token.length() : 0);
        return new RequestSpecBuilder()
                .setContentType(ContentType.JSON)
                .addHeader("Authorization", "Bearer " + token)
                .build();
    }
}
