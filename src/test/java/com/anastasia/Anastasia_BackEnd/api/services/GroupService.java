package com.anastasia.Anastasia_BackEnd.api.services;

import com.anastasia.Anastasia_BackEnd.api.config.RequestSpecFactory;
import com.anastasia.Anastasia_BackEnd.model.group.AddUsersToGroupRequest;
import com.anastasia.Anastasia_BackEnd.model.group.BatchInviteRequest;
import com.anastasia.Anastasia_BackEnd.model.group.GroupDTO;
import com.anastasia.Anastasia_BackEnd.model.group.GroupManagerRequest;
import com.anastasia.Anastasia_BackEnd.model.group.RemoveUsersFromGroupRequest;
import io.qameta.allure.Step;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.util.Map;

import static io.restassured.RestAssured.given;

/**
 * API client for group operations.
 */
public class GroupService {

    private static final String BASE_PATH = "/groups";

    @Step("Create group")
    public Response createGroup(RequestSpecification spec, GroupDTO payload) {
        return given()
                .spec(spec)
                .body(payload)
                .when()
                .post(BASE_PATH)
                .then()
                .extract()
                .response();
    }

    public Response listGroups(RequestSpecification spec, Map<String, Object> queryParams) {
        var request = given().spec(spec);
        if (queryParams != null) {
            queryParams.forEach(request::queryParam);
        }
        return request
                .when()
                .get(BASE_PATH)
                .then()
                .extract()
                .response();
    }

    public Response getGroup(RequestSpecification spec, Long groupId) {
        return given()
                .spec(spec)
                .when()
                .get(BASE_PATH + "/" + groupId)
                .then()
                .extract()
                .response();
    }

    public Response updateGroup(RequestSpecification spec, Long groupId, GroupDTO payload) {
        return given()
                .spec(spec)
                .body(payload)
                .when()
                .put(BASE_PATH + "/" + groupId)
                .then()
                .extract()
                .response();
    }

    public Response addUsers(RequestSpecification spec, Long groupId, AddUsersToGroupRequest payload) {
        return given()
                .spec(spec)
                .body(payload)
                .when()
                .post(BASE_PATH + "/" + groupId + "/users")
                .then()
                .extract()
                .response();
    }

    public Response listGroupMembers(RequestSpecification spec, Long groupId, Map<String, Object> queryParams) {
        var request = given().spec(spec);
        if (queryParams != null) {
            queryParams.forEach(request::queryParam);
        }
        return request
                .when()
                .get(BASE_PATH + "/group/" + groupId + "/members")
                .then()
                .extract()
                .response();
    }

    public Response getGroupMember(RequestSpecification spec, Long groupId, String userId) {
        return given()
                .spec(spec)
                .when()
                .get(BASE_PATH + "/group/members/" + userId)
                .then()
                .extract()
                .response();
    }

    public Response removeMembers(RequestSpecification spec, Long groupId, RemoveUsersFromGroupRequest payload) {
        return given()
                .spec(spec)
                .body(payload)
                .when()
                .delete(BASE_PATH + "/" + groupId + "/members")
                .then()
                .extract()
                .response();
    }

    public Response deleteGroup(RequestSpecification spec, Long groupId) {
        return given()
                .spec(spec)
                .when()
                .delete(BASE_PATH + "/" + groupId)
                .then()
                .extract()
                .response();
    }

    public Response listGroupManagers(RequestSpecification spec, Long groupId) {
        return given()
                .spec(spec)
                .when()
                .get(BASE_PATH + "/" + groupId + "/managers")
                .then()
                .extract()
                .response();
    }

    public Response addManagers(RequestSpecification spec, Long groupId, GroupManagerRequest payload) {
        return given()
                .spec(spec)
                .body(payload)
                .when()
                .post(BASE_PATH + "/" + groupId + "/managers")
                .then()
                .extract()
                .response();
    }

    public Response removeManagers(RequestSpecification spec, Long groupId, GroupManagerRequest payload) {
        return given()
                .spec(spec)
                .body(payload)
                .when()
                .delete(BASE_PATH + "/" + groupId + "/managers")
                .then()
                .extract()
                .response();
    }

    public Response batchInvite(RequestSpecification spec, Long groupId, BatchInviteRequest payload) {
        return given()
                .spec(spec)
                .body(payload)
                .when()
                .post(BASE_PATH + "/" + groupId + "/batch-invite")
                .then()
                .extract()
                .response();
    }

    public Response listCandidates(RequestSpecification spec, Long groupId) {
        return given()
                .spec(spec)
                .when()
                .get(BASE_PATH + "/" + groupId + "/users/candidates")
                .then()
                .extract()
                .response();
    }
}
