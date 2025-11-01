package com.anastasia.Anastasia_BackEnd.Api.services;

import com.anastasia.Anastasia_BackEnd.Api.config.RequestSpecFactory;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.priest.PriestDTO;
import io.qameta.allure.Step;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import static io.restassured.RestAssured.given;

/**
 * API client for priest endpoints.
 */
public class PriestService {

    private static final String BASE_PATH = "/priests";

    @Step("Register priest")
    public Response registerPriest(RequestSpecification spec, PriestDTO payload) {
        return given()
                .spec(spec)
                .body(payload)
                .when()
                .post(BASE_PATH + "/register")
                .then()
                .extract()
                .response();
    }

    public Response registerPriest(PriestDTO payload) {
        return registerPriest(RequestSpecFactory.authenticatedSpec(), payload);
    }

    public Response listPriests(RequestSpecification spec) {
        return given()
                .spec(spec)
                .when()
                .get(BASE_PATH)
                .then()
                .extract()
                .response();
    }

    public Response getPriest(RequestSpecification spec, Long priestId) {
        return given()
                .spec(spec)
                .when()
                .get(BASE_PATH + "/" + priestId)
                .then()
                .extract()
                .response();
    }

    public Response updatePriest(RequestSpecification spec, Long priestId, PriestDTO payload) {
        return given()
                .spec(spec)
                .body(payload)
                .when()
                .patch(BASE_PATH + "/" + priestId)
                .then()
                .extract()
                .response();
    }

    public Response deletePriest(RequestSpecification spec, Long priestId) {
        return given()
                .spec(spec)
                .when()
                .post(BASE_PATH + "/delete/" + priestId)
                .then()
                .extract()
                .response();
    }
}
