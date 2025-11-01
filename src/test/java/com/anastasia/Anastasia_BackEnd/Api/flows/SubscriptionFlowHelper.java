package com.anastasia.Anastasia_BackEnd.Api.flows;

import com.anastasia.Anastasia_BackEnd.Api.config.ConfigManager;
import com.anastasia.Anastasia_BackEnd.Api.services.AuthService;
import com.anastasia.Anastasia_BackEnd.Api.services.TenantService;
import com.anastasia.Anastasia_BackEnd.Api.factories.TenantDataFactory;
import com.anastasia.Anastasia_BackEnd.core.auth.dto.AuthenticationRequest;
import com.anastasia.Anastasia_BackEnd.core.auth.dto.AuthenticationResponse;
import com.anastasia.Anastasia_BackEnd.core.notification.channel.sms.dto.PhoneVerificationRequest;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantDTO;
import com.anastasia.Anastasia_BackEnd.common.utils.JwtUtil;
import io.restassured.response.Response;
import org.junit.jupiter.api.Assertions;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.restassured.RestAssured.given;

public final class SubscriptionFlowHelper {

    private static final TenantService tenantService = new TenantService();
    private static final AuthService authService = new AuthService();
    private static final JwtUtil jwtUtil = new JwtUtil();
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

        // 2 Capture OTP from test endpoint
        String otp = fetchOtpWithRetry(tenantRequest.getPhoneNumber());

        // 3 Verify phone
        PhoneVerificationRequest phoneVerificationRequest = PhoneVerificationRequest.builder()
                .phone(tenantRequest.getPhoneNumber())
                .otp(otp)
                .build();

        Response verifyResponse = tenantService.verifyPhone(phoneVerificationRequest);
        Assertions.assertEquals(200, verifyResponse.statusCode(),
                "Phone verification failed: " + verifyResponse.asString());

        // 4️ Activate account
        String activationToken = fetchActivationToken(tenantRequest.getEmail());
        Response activationResponse = authService.activateAccount(activationToken);
        Assertions.assertEquals(200, activationResponse.statusCode(),
                "Account activation failed: " + activationResponse.asString());

        // 5 Login as OWNER
        AuthenticationResponse authentication = authenticateOwner(tenantRequest);
        cacheTenant(tenantRequest);
        return new SubscriptionResult(tenantRequest, authentication);
    }

    private static String fetchActivationToken(String email) {
        String activationEndpoint = resolveConfigPath("test.activation.endpoint", "/auth/test/activation-token");
        Response response = given()
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

    private static String fetchOtpWithRetry(String phone) {
        String otpEndpoint = resolveConfigPath("test.tenant.otp.endpoint", "/tenant/test/otp");
        int attempts = 0;
        while (attempts < 5) {
            Response response = given()
                    .queryParam("phone", phone)
                    .get(otpEndpoint)
                    .then()
                    .extract()
                    .response();

            if (response.statusCode() == 200 && hasText(response.asString())) {
                return response.asString().trim();
            }

            try {
                Thread.sleep(200L * (attempts + 1));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for OTP", e);
            }
            attempts++;
        }
        Assertions.fail("Failed to capture OTP for phone " + phone);
        return null; // unreachable
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
                new AuthenticationRequest(tenant.getEmail(), tenant.getPassword());
        AuthenticationResponse authentication = authService.loginAndExtractToken(loginRequest);

        Assertions.assertNotNull(authentication, "Authentication response must not be null");
        Assertions.assertNotNull(authentication.getAccessToken(), "Access token must not be null");
        Assertions.assertNotNull(authentication.getRefreshToken(), "Refresh token must not be null");

        List<String> roles = jwtUtil.extractRoles(authentication.getAccessToken());
        Assertions.assertTrue(roles.contains("ROLE_OWNER"),
                "Expected ROLE_OWNER but received roles: " + roles);

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
                .tenantType(source.getTenantType())
                .subscriptionPlan(source.getSubscriptionPlan())
                .ownerName(source.getOwnerName())
                .email(source.getEmail())
                .phoneNumber(source.getPhoneNumber())
                .password(source.getPassword())
                .confirmPassword(source.getConfirmPassword())
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
