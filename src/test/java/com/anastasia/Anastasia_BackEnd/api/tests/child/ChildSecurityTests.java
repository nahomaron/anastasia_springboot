package com.anastasia.Anastasia_BackEnd.api.tests.child;

import com.anastasia.Anastasia_BackEnd.api.base.BaseApiTest;
import com.anastasia.Anastasia_BackEnd.api.factories.ChildDataFactory;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@Epic("Child Registration")
@Feature("Security")
@Severity(SeverityLevel.CRITICAL)
@Owner("API Guild")
class ChildSecurityTests extends BaseApiTest {

    @Test
    void anonymousUserCannotRegisterChild() {
        var response = given()
                .contentType(ContentType.JSON)
                .body(ChildDataFactory.newValidChild())
                .when()
                .post("/registrar/children/register-child")
                .then()
                .extract()
                .response();

        assertThat(response.statusCode()).isEqualTo(403);
    }

    @Test
    void basicUserCannotListChildren() {
        var response = given()
                .spec(getSpecForRole("USER"))
                .when()
                .get("/registrar/children")
                .then()
                .extract()
                .response();

        assertThat(response.statusCode()).isEqualTo(403);
    }
}
