package com.anastasia.Anastasia_BackEnd.api.tests.tenant;

import com.anastasia.Anastasia_BackEnd.api.base.BaseApiTest;
import com.anastasia.Anastasia_BackEnd.api.config.ConfigManager;
import com.anastasia.Anastasia_BackEnd.api.factories.TenantDataFactory;
import com.anastasia.Anastasia_BackEnd.api.services.AuthService;
import com.anastasia.Anastasia_BackEnd.api.services.TenantService;
import com.anastasia.Anastasia_BackEnd.model.auth.AuthenticationRequest;
import com.anastasia.Anastasia_BackEnd.model.auth.AuthenticationResponse;
import com.anastasia.Anastasia_BackEnd.model.sms.PhoneVerificationRequest;
import com.anastasia.Anastasia_BackEnd.model.tenant.TenantDTO;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@Epic("Tenant Subscription")
@Feature("Happy flows")
@Severity(SeverityLevel.CRITICAL)
class TenantPositiveTests extends BaseApiTest {

    private final TenantService tenantService = new TenantService();
    private final AuthService authService = new AuthService();

    @Test
    @Story("Tenant subscription end-to-end flow issues owner tokens")
    void tenantSubscriptionFlowShouldIssueTokens() {
        TenantDTO tenant = TenantDataFactory.newValidTenant();
        Response subscribeResponse = tenantService.subscribeTenant(tenant);
        assertThat(subscribeResponse.statusCode()).isEqualTo(201);

        String otpEndpoint = ConfigManager.get("test.tenant.otp.endpoint");
        String otp = given()
                .queryParam("phone", tenant.getPhoneNumber())
                .when()
                .get(otpEndpoint != null ? otpEndpoint : "/tenant/test/otp")
                .then()
                .extract()
                .asString()
                .trim();
        assertThat(otp).isNotBlank();

        PhoneVerificationRequest phoneVerificationRequest = PhoneVerificationRequest.builder()
                .phone(tenant.getPhoneNumber())
                .otp(otp)
                .build();
        Response verifyResponse = tenantService.verifyPhone(phoneVerificationRequest);
        assertThat(verifyResponse.statusCode()).isEqualTo(200);

        String activationEndpoint = ConfigManager.get("test.activation.endpoint");
        String token = given()
                .queryParam("email", tenant.getEmail())
                .when()
                .get(activationEndpoint != null ? activationEndpoint : "/auth/test/activation-token")
                .then()
                .extract()
                .asString()
                .trim();
        assertThat(token).isNotBlank();

        Response activationResponse = authService.activateAccount(token);
        assertThat(activationResponse.statusCode()).isEqualTo(200);

        AuthenticationResponse login = authService.loginAndExtractToken(new AuthenticationRequest(
                tenant.getEmail(), tenant.getPassword()));
        assertThat(login).isNotNull();
        assertThat(login.getAccessToken()).isNotBlank();
        assertThat(login.getRefreshToken()).isNotBlank();
    }

    @Test
    @Story("Platform admin can list tenants")
    void platformAdminListsTenants() {
        Response response = tenantService.listTenants(getSpecForRole("PLATFORM_ADMIN"));
        assertThat(response.statusCode()).isEqualTo(200);
    }
}
