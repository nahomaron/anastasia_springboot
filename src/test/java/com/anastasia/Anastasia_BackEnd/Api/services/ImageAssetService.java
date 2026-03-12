package com.anastasia.Anastasia_BackEnd.Api.services;

import com.anastasia.Anastasia_BackEnd.Api.config.RequestSpecFactory;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.imageasset.ImageAssetDTO;
import io.qameta.allure.Step;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.util.UUID;

import static io.restassured.RestAssured.given;

/**
 * API client for image asset endpoints.
 */
public class ImageAssetService {

    private static final String BASE_PATH = "/images";

    public Response requestPresignedUrl(String fileName) {
        return requestPresignedUrl(RequestSpecFactory.authenticatedSpec(), fileName);
    }

    @Step("Request image asset presigned URL for {fileName}")
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

    public Response saveAvatar(String ownerType, UUID ownerId, ImageAssetDTO payload) {
        return saveAvatar(RequestSpecFactory.authenticatedSpec(), ownerType, ownerId, payload);
    }

    @Step("Save image asset for {ownerType} {ownerId}")
    public Response saveAvatar(RequestSpecification spec, String ownerType, UUID ownerId, ImageAssetDTO payload) {
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

    @Step("Fetch image asset for {ownerType} {ownerId}")
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
