package com.anastasia.Anastasia_BackEnd.api.services;

import com.anastasia.Anastasia_BackEnd.api.config.RequestSpecFactory;
import com.anastasia.Anastasia_BackEnd.model.child.ChildDTO;
import io.qameta.allure.Step;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.util.Map;

import static io.restassured.RestAssured.given;

/**
 * API client for child registration endpoints.
 */
public class ChildService {

    private static final String BASE_PATH = "/registrar/children";

    @Step("Register child")
    public Response registerChild(RequestSpecification spec, ChildDTO payload) {
        return given()
                .spec(spec)
                .body(payload)
                .when()
                .post(BASE_PATH + "/register-child")
                .then()
                .extract()
                .response();
    }

    public Response registerChild(ChildDTO payload) {
        return registerChild(RequestSpecFactory.authenticatedSpec(), payload);
    }

    public Response listChildren(RequestSpecification spec, Map<String, Object> queryParams) {
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

    public Response listChildren() {
        return listChildren(RequestSpecFactory.authenticatedSpec(), null);
    }

    public Response getChild(RequestSpecification spec, Long memberId) {
        return given()
                .spec(spec)
                .when()
                .get(BASE_PATH + "/" + memberId)
                .then()
                .extract()
                .response();
    }

    public Response getChild(Long memberId) {
        return getChild(RequestSpecFactory.authenticatedSpec(), memberId);
    }

    @Step("Update child {memberId}")
    public Response updateChild(RequestSpecification spec, Long memberId, ChildDTO payload) {
        return given()
                .spec(spec)
                .body(payload)
                .when()
                .patch(BASE_PATH + "/" + memberId)
                .then()
                .extract()
                .response();
    }

    public Response deleteChild(RequestSpecification spec, Long memberId) {
        return given()
                .spec(spec)
                .when()
                .delete(BASE_PATH + "/" + memberId)
                .then()
                .extract()
                .response();
    }

    public Response advancedSearch(RequestSpecification spec, Map<String, Object> queryParams, Object addressBody) {
        var request = given().spec(spec);
        if (queryParams != null) {
            queryParams.forEach(request::queryParam);
        }
        if (addressBody != null) {
            request.body(addressBody);
        }
        return request
                .when()
                .post(BASE_PATH + "/advanced-search")
                .then()
                .extract()
                .response();
    }
}
