package com.anastasia.Anastasia_BackEnd.Api.tests.tenant;

import com.anastasia.Anastasia_BackEnd.Api.base.BaseApiTest;
import com.anastasia.Anastasia_BackEnd.Api.config.ConfigManager;
import com.anastasia.Anastasia_BackEnd.Api.config.RequestSpecFactory;
import com.anastasia.Anastasia_BackEnd.Api.factories.TenantDataFactory;
import com.anastasia.Anastasia_BackEnd.Api.services.AuthService;
import com.anastasia.Anastasia_BackEnd.Api.services.TenantService;
import com.anastasia.Anastasia_BackEnd.core.auth.dto.AuthenticationRequest;
import com.anastasia.Anastasia_BackEnd.core.auth.dto.AuthenticationResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantDTO;
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
public class TenantPositiveTests extends BaseApiTest {

    private final TenantService tenantService = new TenantService();
    private final AuthService authService = new AuthService();

    @Test
    @Story("Tenant subscription end-to-end flow issues owner tokens")
    public void tenantSubscriptionFlowShouldIssueTokens() {
        TenantDTO tenant = TenantDataFactory.newValidTenant();
        Response subscribeResponse = tenantService.subscribeTenant(tenant);
        assertThat(subscribeResponse.statusCode()).isEqualTo(201);

        Response verifyResponse = tenantService.verifyPhone(tenant.getPhoneNumber(), "disabled");
        assertThat(verifyResponse.statusCode()).isEqualTo(200);
        assertThat(verifyResponse.asString()).contains("disabled");

        String activationEndpoint = ConfigManager.get("test.activation.endpoint");
        String token = given()
                .spec(RequestSpecFactory.testHelperSpec())
                .queryParam("email", tenant.getOwnerEmail())
                .when()
                .get(activationEndpoint != null ? activationEndpoint : "/auth/test/activation-token")
                .then()
                .extract()
                .asString()
                .trim();
        assertThat(token).isNotBlank();

        Response activationResponse = authService.activateAccount(token, tenant.getOwnerEmail());
        assertThat(activationResponse.statusCode()).isEqualTo(200);

        AuthenticationResponse login = authService.loginAndExtractToken(new AuthenticationRequest(
                tenant.getOwnerEmail(), tenant.getPassword()));
        assertThat(login).isNotNull();
        assertThat(login.getAccessToken()).isNotBlank();
        assertThat(login.getRefreshToken()).isNotBlank();
    }

    @Test
    @Story("Platform admin can list tenants")
    public void platformAdminListsTenants() {
        Response response = tenantService.listTenants(getSpecForRole("PLATFORM_ADMIN"));
        assertThat(response.statusCode()).isEqualTo(200);
    }
}
