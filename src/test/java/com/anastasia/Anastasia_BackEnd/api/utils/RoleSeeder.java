package com.anastasia.Anastasia_BackEnd.api.utils;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;

/**
 * RoleSeeder:
 * Utility to assign roles to users through the secured /assign-roles endpoint.
 * Operates entirely via REST APIs (no repository access).
 */
@Slf4j
public final class RoleSeeder {

    private RoleSeeder() {}

    /**
     * Assigns roles to a user identified by email using a privileged caller token.
     *
     * @param adminOrOwnerToken bearer token with manage-roles capability
     * @param userEmail         email of the target user
     * @param roleIds           set of role IDs to assign
     */
    public static void assignRolesToUser(String adminOrOwnerToken, String userEmail, Set<Long> roleIds) {
        log.info("Assigning roles {} to user [{}]", roleIds, userEmail);

        UUID userId = fetchUserIdByEmail(userEmail);
        if (userId == null) {
            throw new RuntimeException("User not found for email: " + userEmail);
        }

        String roleIdsBody = roleIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining("\", \"", "\"", "\""));

        String body = String.format("""
            { "roleIds": [%s] }
            """, roleIds.stream().map(String::valueOf).collect(Collectors.joining(", ")));

        Response res = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + adminOrOwnerToken)
                .body(body)
                .when()
                .put("/users/" + userId + "/assign-roles")
                .then()
                .extract().response();

        if (res.statusCode() != 200) {
            throw new RuntimeException("Role assignment failed: " + res.statusCode() + " " + res.asString());
        }

        log.info("✅ Successfully assigned roles {} to user [{}]", roleIdsBody, userEmail);
    }

    private static UUID fetchUserIdByEmail(String email) {
        Response res = given()
                .header("Authorization", "Bearer " + RoleContextFactory.ownerToken())
                .queryParam("email", email)
                .get("/test/users/id")
                .then()
                .extract()
                .response();

        if (res.statusCode() == 200 && res.jsonPath().get("id") != null) {
            return UUID.fromString(res.jsonPath().getString("id"));
        } else if (res.statusCode() == 404) {
            log.warn("User with email [{}] not found (404)", email);
            return null;
        } else {
            throw new RuntimeException("Unexpected error while fetching user ID: "
                    + res.statusCode() + " " + res.asString());
        }
    }
}
