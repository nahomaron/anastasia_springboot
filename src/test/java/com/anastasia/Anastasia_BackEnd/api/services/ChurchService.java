package com.anastasia.Anastasia_BackEnd.api.services;

import com.anastasia.Anastasia_BackEnd.api.config.RequestSpecFactory;
import com.anastasia.Anastasia_BackEnd.model.church.ChurchDTO;
import io.qameta.allure.Step;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import static io.restassured.RestAssured.given;

/**
 * API client for church endpoints.
 */
public class ChurchService {

    private static final String BASE_PATH = "/churches";

    @Step("Register church")
    public Response registerChurch(RequestSpecification spec, ChurchDTO payload) {
        return given()
                .spec(spec)
                .body(payload)
                .when()
                .post(BASE_PATH + "/register")
                .then()
                .extract()
                .response();
    }

    public Response registerChurch(ChurchDTO payload) {
        return registerChurch(RequestSpecFactory.authenticatedSpec(), payload);
    }

    public Response listChurches(RequestSpecification spec) {
        return given()
                .spec(spec)
                .when()
                .get(BASE_PATH)
                .then()
                .extract()
                .response();
    }

    public Response listChurches() {
        return listChurches(RequestSpecFactory.authenticatedSpec());
    }

    public Response getChurch(RequestSpecification spec, Long churchId) {
        return given()
                .spec(spec)
                .when()
                .get(BASE_PATH + "/" + churchId)
                .then()
                .extract()
                .response();
    }

    public Response updateChurch(RequestSpecification spec, Long churchId, ChurchDTO payload) {
        return given()
                .spec(spec)
                .body(payload)
                .when()
                .put(BASE_PATH + "/" + churchId)
                .then()
                .extract()
                .response();
    }

    public Response deleteChurch(RequestSpecification spec, Long churchId) {
        return given()
                .spec(spec)
                .when()
                .delete(BASE_PATH + "/" + churchId)
                .then()
                .extract()
                .response();
    }
}
