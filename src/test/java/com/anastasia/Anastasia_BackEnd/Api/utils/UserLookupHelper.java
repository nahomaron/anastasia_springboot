package com.anastasia.Anastasia_BackEnd.Api.utils;

import io.restassured.response.Response;

import java.util.Optional;
import java.util.UUID;

import static io.restassured.RestAssured.given;

/**
 * Utility for querying the test lookup endpoints.
 */
public final class UserLookupHelper {

    private UserLookupHelper() {
    }

    public static Optional<UUID> findUserIdByEmail(String email) {
        Response response = given()
                .queryParam("email", email)
                .when()
                .get("/test/users/id")
                .then()
                .extract()
                .response();

        if (response.statusCode() == 200) {
            String id = response.jsonPath().getString("id");
            if (id != null && !id.isBlank()) {
                return Optional.of(UUID.fromString(id));
            }
        }
        return Optional.empty();
    }
}
