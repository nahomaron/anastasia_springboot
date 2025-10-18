package com.anastasia.Anastasia_BackEnd.api.tests.member;

import com.anastasia.Anastasia_BackEnd.api.base.BaseApiTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class MemberNegativeTests extends BaseApiTest {

    @Test
    @DisplayName("400 – Missing required fields returns BAD REQUEST")
    void createMember_MissingFields_ShouldReturn400() {
        var invalidBody = """
            { "firstName": "John" }
            """;

        given()
                .spec(getSpecForRole("PRIEST"))
                .contentType(ContentType.JSON)
                .body(invalidBody)
                .when()
                .post("/registrar/members/register-member")
                .then()
                .statusCode(400)
                .body("errors", not(empty()));
    }

    @Test
    @DisplayName("404 – Non-existent member ID")
    void getMember_NotFound_ShouldReturn404() {
        given()
                .spec(getSpecForRole("PRIEST"))
                .when()
                .get("/registrar/members/999999")
                .then()
                .statusCode(404);
    }
}
