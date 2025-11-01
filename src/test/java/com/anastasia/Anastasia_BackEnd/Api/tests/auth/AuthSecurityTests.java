package com.anastasia.Anastasia_BackEnd.Api.tests.auth;

import com.anastasia.Anastasia_BackEnd.Api.base.BaseApiTest;
import io.qameta.allure.*;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

@Epic("Authentication")
@Feature("Access Control & Security")
@Owner("Nahom Aron")
@Severity(SeverityLevel.CRITICAL)
public class AuthSecurityTests extends BaseApiTest {

    @Test
    @DisplayName("403 – Missing JWT")
    void accessWithoutToken_ShouldReturn401() {
        given()
                .contentType(ContentType.JSON)
                .when()
                .get("/registrar/members")
                .then()
                .statusCode(403);
    }

    @Test
    @DisplayName("403 – USER cannot list members")
    void userRoleCannotAccessMembersList() {
        given()
                .spec(getSpecForRole("USER"))
                .when()
                .get("/registrar/members")
                .then()
                .statusCode(403);
    }

    @Test
    @DisplayName("403 – USER cannot get member by ID")
    void userRoleCannotAccessMemberById() {
        given()
                .spec(getSpecForRole("USER"))
                .when()
                .get("/registrar/members/1")
                .then()
                .statusCode(403);
    }


}
