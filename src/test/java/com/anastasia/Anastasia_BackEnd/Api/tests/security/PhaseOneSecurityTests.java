package com.anastasia.Anastasia_BackEnd.Api.tests.security;

import com.anastasia.Anastasia_BackEnd.Api.base.BaseApiTest;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@Epic("Authorization Hardening")
@Feature("Phase 1")
@Severity(SeverityLevel.CRITICAL)
@Owner("API Guild")
class PhaseOneSecurityTests extends BaseApiTest {

    @Test
    void anonymousCannotCreateAccountingAccount() {
        var response = given()
                .contentType(ContentType.JSON)
                .body("{}")
                .when()
                .post("/accounting/accounts")
                .then()
                .extract()
                .response();

        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    void anonymousCannotGenerateAccountingReport() {
        var response = given()
                .contentType(ContentType.JSON)
                .body("{}")
                .when()
                .post("/accounting/reports/generate")
                .then()
                .extract()
                .response();

        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    void anonymousCannotViewOnboardingStripeHealth() {
        var response = given()
                .when()
                .get("/onboarding/billing/health/stripe")
                .then()
                .extract()
                .response();

        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    void anonymousCannotReadCurrentTenantVerificationStatus() {
        var response = given()
                .when()
                .get("/tenant/current/verification-status")
                .then()
                .extract()
                .response();

        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    void anonymousCanStillReachPublicTenantPhoneVerificationRoute() {
        var response = given()
                .contentType(ContentType.JSON)
                .body("{}")
                .when()
                .post("/tenant/verify-phone")
                .then()
                .extract()
                .response();

        assertThat(response.statusCode()).isEqualTo(400);
    }
}
