package com.anastasia.Anastasia_BackEnd.Api.tests.church;

import com.anastasia.Anastasia_BackEnd.Api.base.BaseApiTest;
import com.anastasia.Anastasia_BackEnd.Api.factories.ChurchDataFactory;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@Epic("Church Management")
@Feature("Security")
@Severity(SeverityLevel.CRITICAL)
@Owner("API Guild")
class ChurchSecurityTests extends BaseApiTest {

    @Test
    void anonymousCannotRegisterChurch() {
        var response = given()
                .contentType(ContentType.JSON)
                .body(ChurchDataFactory.newValidChurch())
                .when()
                .post("/churches/register")
                .then()
                .extract()
                .response();

        assertThat(response.statusCode()).isEqualTo(403);
    }

    @Test
    void ownerCannotListAllChurches() {
        var response = given()
                .spec(getSpecForRole("OWNER"))
                .when()
                .get("/churches")
                .then()
                .extract()
                .response();

        assertThat(response.statusCode()).isEqualTo(403);
    }
}
