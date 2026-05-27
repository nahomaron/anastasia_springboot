package com.anastasia.Anastasia_BackEnd.Api.flows;

import com.anastasia.Anastasia_BackEnd.Api.config.ConfigManager;
import com.anastasia.Anastasia_BackEnd.Api.config.RequestSpecFactory;
import com.anastasia.Anastasia_BackEnd.Api.services.AuthService;
import com.anastasia.Anastasia_BackEnd.Api.services.TenantService;
import com.anastasia.Anastasia_BackEnd.Api.factories.TenantDataFactory;
import com.anastasia.Anastasia_BackEnd.core.auth.dto.AuthenticationRequest;
import com.anastasia.Anastasia_BackEnd.core.auth.dto.AuthenticationResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantDTO;
import io.restassured.response.Response;
import org.junit.jupiter.api.Assertions;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.restassured.RestAssured.given;

public final class SubscriptionFlowHelper {
    private static final Logger log = LoggerFactory.getLogger(SubscriptionFlowHelper.class);
    private static final TenantService tenantService = new TenantService();
    private static final AuthService authService = new AuthService();
    private static final Map<String, TenantDTO> tenantCache = new ConcurrentHashMap<>();

    private SubscriptionFlowHelper() {
        // utility
    }

    public static SubscriptionResult subscribeTenantAndLoginOwner() {
        return subscribeTenantAndLoginOwner(TenantDataFactory.newValidTenant());
    }

    public static SubscriptionResult subscribeTenantAndLoginOwner(TenantDTO tenantRequest) {

        SubscriptionResult existingTenant = findExistingTenant(tenantRequest);
        if (existingTenant != null) {
            return existingTenant;
        }

        // 1 Subscribe tenant
        Response subscriptionResponse = tenantService.subscribeTenant(tenantRequest);
        Assertions.assertEquals(201, subscriptionResponse.statusCode(),
                "Tenant subscription failed: " + subscriptionResponse.asString());

        // 2 Keep the compatibility verification hook in the flow, even though OTP is disabled.
        Response verifyResponse = tenantService.verifyPhone(tenantRequest.getPhoneNumber(), "disabled");
        Assertions.assertEquals(200, verifyResponse.statusCode(),
                "Phone verification failed: " + verifyResponse.asString());

        // 3 Activate account
        String activationToken = fetchActivationToken(tenantRequest.getOwnerEmail());
        Response activationResponse = authService.activateAccount(activationToken, tenantRequest.getOwnerEmail());
        Assertions.assertEquals(200, activationResponse.statusCode(),
                "Account activation failed: " + activationResponse.asString());

        // 4 Login as OWNER
        AuthenticationResponse authentication = authenticateOwner(tenantRequest);
        cacheTenant(tenantRequest);
        return new SubscriptionResult(tenantRequest, authentication);
    }

    private static String fetchActivationToken(String email) {
        String activationEndpoint = resolveEndpointPath("test.activation.endpoint", "/auth/test/activation-token");
        Response response = given()
                .spec(RequestSpecFactory.testHelperSpec())
                .queryParam("email", email)
                .get(activationEndpoint)
                .then()
                .extract()
                .response();
        Assertions.assertEquals(200, response.statusCode(),
                "Failed to fetch activation token: " + response.asString());
        String token = response.asString();
        Assertions.assertTrue(hasText(token), "Activation token must not be blank");
        return token.trim();
    }

    private static String fetchRefreshToken(String email) {
        String refreshEndpoint = resolveEndpointPath("test.refresh.endpoint", "/auth/test/refresh-token");
        Response response = given()
                .spec(RequestSpecFactory.testHelperSpec())
                .queryParam("email", email)
                .get(refreshEndpoint)
                .then()
                .extract()
                .response();
        Assertions.assertEquals(200, response.statusCode(),
                "Failed to fetch refresh token: " + response.asString());
        String token = response.asString();
        Assertions.assertTrue(hasText(token), "Refresh token must not be blank");
        return token.trim();
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static String resolveConfigPath(String key, String fallback) {
        String value = ConfigManager.get(key);
        if (hasText(value)) {
            return value.trim();
        }
        return fallback;
    }

    private static String resolveEndpointPath(String key, String fallback) {
        return normalizeEndpointPath(resolveConfigPath(key, fallback));
    }

    private static String normalizeEndpointPath(String path) {
        if (!hasText(path)) {
            return path;
        }
        String normalized = path.trim();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        normalized = normalized.replaceAll("/{2,}", "/");

        return normalized;
    }

    private static SubscriptionResult findExistingTenant(TenantDTO incomingTenant) {
        if (incomingTenant == null || !hasText(incomingTenant.getPhoneNumber())) {
            return null;
        }

        TenantDTO cachedTenant = tenantCache.get(incomingTenant.getPhoneNumber());
        if (cachedTenant == null) {
            return null;
        }

        AuthenticationResponse authentication = authenticateOwner(cachedTenant);
        return new SubscriptionResult(cachedTenant, authentication);
    }

    private static AuthenticationResponse authenticateOwner(TenantDTO tenant) {
        Assertions.assertNotNull(tenant, "Tenant details are required for authentication");
        AuthenticationRequest loginRequest =
                new AuthenticationRequest(tenant.getOwnerEmail(), tenant.getPassword());
        AuthenticationResponse authentication = authService.loginAndExtractToken(loginRequest);

        Assertions.assertNotNull(authentication, "Authentication response must not be null");
        Assertions.assertNotNull(authentication.getAccessToken(), "Access token must not be null");
        String refreshToken = fetchRefreshToken(tenant.getOwnerEmail());
        authentication.setRefreshToken(refreshToken);
        Assertions.assertNotNull(authentication.getRefreshToken(), "Refresh token must not be null");

        log.info("Authenticated owner via email {}", tenant.getOwnerEmail());

        return authentication;
    }

    private static void cacheTenant(TenantDTO tenant) {
        if (tenant == null || !hasText(tenant.getPhoneNumber())) {
            return;
        }
        tenantCache.put(tenant.getPhoneNumber(), copyOf(tenant));
    }

    private static TenantDTO copyOf(TenantDTO source) {
        if (source == null) {
            return null;
        }
        return TenantDTO.builder()
                .displayName(source.getDisplayName())
                .slug(source.getSlug())
                .status(source.getStatus())
                .tenantType(source.getTenantType())
                .subscriptionPlan(source.getSubscriptionPlan())
                .ownerName(source.getOwnerName())
                .ownerEmail(source.getOwnerEmail())
                .phoneNumber(source.getPhoneNumber())
                .password(source.getPassword())
                .confirmPassword(source.getConfirmPassword())
                .church(source.getChurch())
                .build();
    }

    public record SubscriptionResult(TenantDTO tenantRequest, AuthenticationResponse authResponse) {
        public String accessToken() {
            return authResponse.getAccessToken();
        }

        public String refreshToken() {
            return authResponse.getRefreshToken();
        }
    }
}
