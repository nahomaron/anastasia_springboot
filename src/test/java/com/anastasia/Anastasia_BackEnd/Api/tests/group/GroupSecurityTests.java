package com.anastasia.Anastasia_BackEnd.Api.tests.group;

import com.anastasia.Anastasia_BackEnd.Api.base.BaseApiTest;
import com.anastasia.Anastasia_BackEnd.Api.factories.GroupDataFactory;
import com.anastasia.Anastasia_BackEnd.modules.groups.dto.GroupManagerRequest;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Set;

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

    @Test
    void userCannotManageGroupManagers() {
        GroupManagerRequest request = GroupManagerRequest.builder().managerIds(Set.of(java.util.UUID.randomUUID())).build();
        var response = given()
                .spec(getSpecForRole("USER"))
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/groups/1/managers")
                .then()
                .extract()
                .response();

        assertThat(response.statusCode()).isEqualTo(403);
    }
}
