package com.anastasia.Anastasia_BackEnd.Api.services;

import com.anastasia.Anastasia_BackEnd.core.auth.dto.ChangePasswordRequest;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.avatar.AvatarDTO;
import com.anastasia.Anastasia_BackEnd.core.auth.role.AssignRolesRequest;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserDTO;
import io.qameta.allure.Step;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;

/**
 * API client for user management endpoints.
 */
public class UserService {

    private static final String BASE_PATH = "/users";

    public Response getDashboard(RequestSpecification spec) {
        return given()
                .spec(spec)
                .when()
                .get(BASE_PATH + "/dashboard")
                .then()
                .extract()
                .response();
    }

    public Response listUsers(RequestSpecification spec, Map<String, Object> queryParams) {
        var request = given().spec(spec);
        if (queryParams != null) {
            queryParams.forEach(request::queryParam);
        }
        return request
                .when()
                .get(BASE_PATH + "/")
                .then()
                .extract()
                .response();
    }

    public Response getUser(RequestSpecification spec, UUID userId) {
        return given()
                .spec(spec)
                .when()
                .get(BASE_PATH + "/" + userId)
                .then()
                .extract()
                .response();
    }

    @Step("Update profile details")
    public Response updateDetails(RequestSpecification spec, UserDTO payload) {
        return given()
                .spec(spec)
                .body(payload)
                .when()
                .patch(BASE_PATH + "/update-user-details")
                .then()
                .extract()
                .response();
    }

    public Response updateAvatar(RequestSpecification spec, AvatarDTO payload) {
        return given()
                .spec(spec)
                .body(payload)
                .when()
                .put(BASE_PATH + "/avatar")
                .then()
                .extract()
                .response();
    }

    public Response assignRoles(RequestSpecification spec, UUID userId, AssignRolesRequest payload) {
        return given()
                .spec(spec)
                .body(payload)
                .when()
                .put(BASE_PATH + "/" + userId + "/assign-roles")
                .then()
                .extract()
                .response();
    }

    public Response changePassword(RequestSpecification spec, ChangePasswordRequest payload) {
        return given()
                .spec(spec)
                .body(payload)
                .when()
                .patch(BASE_PATH + "/change-password")
                .then()
                .extract()
                .response();
    }

    public Response deleteUser(RequestSpecification spec, UUID userId) {
        return given()
                .spec(spec)
                .when()
                .delete(BASE_PATH + "/" + userId)
                .then()
                .extract()
                .response();
    }
}
