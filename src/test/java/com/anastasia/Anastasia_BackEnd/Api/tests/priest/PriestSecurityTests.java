package com.anastasia.Anastasia_BackEnd.Api.tests.priest;

import com.anastasia.Anastasia_BackEnd.Api.base.BaseApiTest;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@Epic("Priest Management")
@Feature("Security")
@Severity(SeverityLevel.CRITICAL)
@Owner("API Guild")
class PriestSecurityTests extends BaseApiTest {

//    @Test
//    void anonymousCannotRegisterPriest() {
//        var response = given()
//                .contentType(ContentType.JSON)
//                .body(PriestDataFactory.newValidPriest("AB12345", null))
//                .when()
//                .post("/priests/register")
//                .then()
//                .extract()
//                .response();
//
//        assertThat(response.statusCode()).isEqualTo(403);
//    }

    @Test
    void userCannotListPriests() {
        var response = given()
                .spec(getSpecForRole("USER"))
                .when()
                .get("/priests")
                .then()
                .extract()
                .response();

        assertThat(response.statusCode()).isEqualTo(403);
    }
}
