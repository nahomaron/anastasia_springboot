package com.anastasia.Anastasia_BackEnd.api.tests.tenant;

import com.anastasia.Anastasia_BackEnd.api.base.BaseApiTest;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@Epic("Tenant Subscription")
@Feature("Security")
@Severity(SeverityLevel.CRITICAL)
@Owner("API Guild")
class TenantSecurityTests extends BaseApiTest {

    @Test
    void anonymousCannotListTenants() {
        var response = given()
                .when()
                .get("/tenant")
                .then()
                .extract()
                .response();

        assertThat(response.statusCode()).isEqualTo(403);
    }

    @Test
    void userCannotUnsubscribeTenant() {
        var response = given()
                .spec(getSpecForRole("USER"))
                .when()
                .post("/tenant/unsubscribe/00000000-0000-0000-0000-000000000000")
                .then()
                .extract()
                .response();

        assertThat(response.statusCode()).isEqualTo(403);
    }
}
