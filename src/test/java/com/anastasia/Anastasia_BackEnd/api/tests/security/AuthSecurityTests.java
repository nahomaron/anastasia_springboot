package com.anastasia.Anastasia_BackEnd.api.tests.security;

import com.anastasia.Anastasia_BackEnd.api.base.BaseApiTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

public class AuthSecurityTests extends BaseApiTest {

    @Test
    @DisplayName("401 – Missing JWT")
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
}
