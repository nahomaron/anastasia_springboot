package com.anastasia.Anastasia_BackEnd.api.services;

import com.anastasia.Anastasia_BackEnd.api.config.RequestSpecFactory;
import com.anastasia.Anastasia_BackEnd.model.avatar.AvatarDTO;
import io.qameta.allure.Step;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.util.UUID;

import static io.restassured.RestAssured.given;

/**
 * API client for avatar endpoints.
 */
public class AvatarService {

    private static final String BASE_PATH = "/avatars";

    public Response requestPresignedUrl(String fileName) {
        return requestPresignedUrl(RequestSpecFactory.authenticatedSpec(), fileName);
    }

    @Step("Request avatar presigned URL for {fileName}")
    public Response requestPresignedUrl(RequestSpecification spec, String fileName) {
        return given()
                .spec(spec)
                .queryParam("fileName", fileName)
                .when()
                .post(BASE_PATH + "/presigned-url")
                .then()
                .extract()
                .response();
    }

    public Response saveAvatar(String ownerType, UUID ownerId, AvatarDTO payload) {
        return saveAvatar(RequestSpecFactory.authenticatedSpec(), ownerType, ownerId, payload);
    }

    @Step("Save avatar for {ownerType} {ownerId}")
    public Response saveAvatar(RequestSpecification spec, String ownerType, UUID ownerId, AvatarDTO payload) {
        return given()
                .spec(spec)
                .body(payload)
                .when()
                .post(BASE_PATH + "/" + ownerType + "/" + ownerId)
                .then()
                .extract()
                .response();
    }

    public Response getAvatar(String ownerType, UUID ownerId) {
        return getAvatar(RequestSpecFactory.authenticatedSpec(), ownerType, ownerId);
    }

    @Step("Fetch avatar for {ownerType} {ownerId}")
    public Response getAvatar(RequestSpecification spec, String ownerType, UUID ownerId) {
        return given()
                .spec(spec)
                .when()
                .get(BASE_PATH + "/" + ownerType + "/" + ownerId)
                .then()
                .extract()
                .response();
    }
}
