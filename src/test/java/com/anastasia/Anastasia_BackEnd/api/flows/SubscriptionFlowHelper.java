package com.anastasia.Anastasia_BackEnd.api.flows;

import com.anastasia.Anastasia_BackEnd.api.config.ConfigManager;
import com.anastasia.Anastasia_BackEnd.api.services.AuthService;
import com.anastasia.Anastasia_BackEnd.api.services.TenantService;
import com.anastasia.Anastasia_BackEnd.api.factories.TenantDataFactory;
import com.anastasia.Anastasia_BackEnd.model.auth.AuthenticationRequest;
import com.anastasia.Anastasia_BackEnd.model.auth.AuthenticationResponse;
import com.anastasia.Anastasia_BackEnd.model.sms.PhoneVerificationRequest;
import com.anastasia.Anastasia_BackEnd.model.tenant.TenantDTO;
import com.anastasia.Anastasia_BackEnd.util.JwtUtil;
import io.restassured.response.Response;
import org.junit.jupiter.api.Assertions;

import java.util.List;

import static io.restassured.RestAssured.given;

public final class SubscriptionFlowHelper {

    private static final TenantService tenantService = new TenantService();
    private static final AuthService authService = new AuthService();
    private static final JwtUtil jwtUtil = new JwtUtil();

    private SubscriptionFlowHelper() {
        // utility
    }

    public static SubscriptionResult subscribeTenantAndLoginOwner() {
        return subscribeTenantAndLoginOwner(TenantDataFactory.newValidTenant());
    }

    public static SubscriptionResult subscribeTenantAndLoginOwner(TenantDTO tenantRequest) {
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
        AuthenticationRequest loginRequest =
                new AuthenticationRequest(tenantRequest.getEmail(), tenantRequest.getPassword());
        AuthenticationResponse authentication = authService.loginAndExtractToken(loginRequest);

        Assertions.assertNotNull(authentication, "Authentication response must not be null");
        Assertions.assertNotNull(authentication.getAccessToken(), "Access token must not be null");
        Assertions.assertNotNull(authentication.getRefreshToken(), "Refresh token must not be null");

        List<String> roles = jwtUtil.extractRoles(authentication.getAccessToken());
        Assertions.assertTrue(roles.contains("ROLE_OWNER"),
                "Expected ROLE_OWNER but received roles: " + roles);

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

    public record SubscriptionResult(TenantDTO tenantRequest, AuthenticationResponse authResponse) {
        public String accessToken() {
            return authResponse.getAccessToken();
        }

        public String refreshToken() {
            return authResponse.getRefreshToken();
        }
    }
}
