package com.anastasia.Anastasia_BackEnd.api.tests.group;

import com.anastasia.Anastasia_BackEnd.api.base.BaseApiTest;
import com.anastasia.Anastasia_BackEnd.api.factories.GroupDataFactory;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@Epic("Group Management")
@Feature("Security")
@Severity(SeverityLevel.CRITICAL)
@Owner("API Guild")
class GroupSecurityTests extends BaseApiTest {

    @Test
    void anonymousCannotCreateGroup() {
        var response = given()
                .contentType(ContentType.JSON)
                .body(GroupDataFactory.newGroup("1"))
                .when()
                .post("/groups")
                .then()
                .extract()
                .response();

        assertThat(response.statusCode()).isEqualTo(403);
    }

    @Test
    void userCannotDeleteGroup() {
        var response = given()
                .spec(getSpecForRole("USER"))
                .when()
                .delete("/groups/1")
                .then()
                .extract()
                .response();

        assertThat(response.statusCode()).isEqualTo(403);
    }
}
