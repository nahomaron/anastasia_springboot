package com.anastasia.Anastasia_BackEnd.Api.tests.user;

import com.anastasia.Anastasia_BackEnd.Api.base.BaseApiTest;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@Epic("User Management")
@Feature("Security")
@Severity(SeverityLevel.CRITICAL)
@Owner("API Guild")
class UserSecurityTests extends BaseApiTest {

    @Test
    void anonymousCannotAccessDashboard() {
        var response = given()
                .when()
                .get("/users/dashboard")
                .then()
                .extract()
                .response();

        assertThat(response.statusCode()).isEqualTo(403);
    }

    @Test
    void userCannotListPlatformUsers() {
        var response = given()
                .spec(getSpecForRole("USER"))
                .when()
                .get("/users/")
                .then()
                .extract()
                .response();

        assertThat(response.statusCode()).isEqualTo(403);
    }

    @Test
    void anonymousCannotUpdateUserDetails() {
        var response = given()
                .contentType(ContentType.JSON)
                .body("{}")
                .when()
                .patch("/users/update-user-details")
                .then()
                .extract()
                .response();

        assertThat(response.statusCode()).isEqualTo(403);
    }

    @Test
    void adminCannotAssignRolesWithoutPermissionGrant() {
        var response = given()
                .spec(getSpecForRole("ADMIN"))
                .contentType(ContentType.JSON)
                .body(Map.of("roles", java.util.List.of()))
                .when()
                .put("/users/" + UUID.randomUUID() + "/assign-roles")
                .then()
                .extract()
                .response();

        assertThat(response.statusCode()).isEqualTo(403);
    }
}
