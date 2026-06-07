package com.anastasia.Anastasia_BackEnd.Api.tests.event;

import com.anastasia.Anastasia_BackEnd.Api.base.BaseApiTest;
import com.anastasia.Anastasia_BackEnd.Api.factories.EventDataFactory;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@Epic("Event Management")
@Feature("Security")
@Severity(SeverityLevel.CRITICAL)
@Owner("API Guild")
class EventSecurityTests extends BaseApiTest {

    @Test
    void anonymousCannotCreateEvent() {
        var response = given()
                .contentType(ContentType.JSON)
                .body(EventDataFactory.newEvent(null))
                .when()
                .post("/events")
                .then()
                .extract()
                .response();

        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    void userRoleCannotAssignManagers() {
        var response = given()
                .spec(getSpecForRole("USER"))
                .contentType(ContentType.JSON)
                .body(EventDataFactory.assignManagerRequest(null, null))
                .when()
                .post("/events/1/managers")
                .then()
                .extract()
                .response();

        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    void anonymousCannotListVisibleEvents() {
        var response = given()
                .when()
                .get("/events/visible")
                .then()
                .extract()
                .response();

        assertThat(response.statusCode()).isEqualTo(403);
    }
}
