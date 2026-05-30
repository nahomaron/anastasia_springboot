package com.anastasia.Anastasia_BackEnd.Api.services;

import com.anastasia.Anastasia_BackEnd.Api.config.RequestSpecFactory;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.imageasset.FinalizeImageUploadRequest;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.imageasset.ImageUploadRequest;
import io.qameta.allure.Step;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import static io.restassured.RestAssured.given;

/**
 * API client for image asset endpoints.
 */
public class ImageAssetService {

    private static final String BASE_PATH = "/images";

    public Response requestPresignedUrl(String ownerType, Object ownerId, ImageUploadRequest request) {
        return requestPresignedUrl(RequestSpecFactory.authenticatedSpec(), ownerType, ownerId, request);
    }

    @Step("Request image asset presigned URL for {ownerType} {ownerId}")
    public Response requestPresignedUrl(RequestSpecification spec, String ownerType, Object ownerId, ImageUploadRequest request) {
        return given()
                .spec(spec)
                .body(request)
                .when()
                .post(BASE_PATH + "/" + ownerType + "/" + ownerId + "/presigned-url")
                .then()
                .extract()
                .response();
    }

    public Response saveAvatar(String ownerType, Object ownerId, FinalizeImageUploadRequest payload) {
        return saveAvatar(RequestSpecFactory.authenticatedSpec(), ownerType, ownerId, payload);
    }

    @Step("Save image asset for {ownerType} {ownerId}")
    public Response saveAvatar(RequestSpecification spec, String ownerType, Object ownerId, FinalizeImageUploadRequest payload) {
        return given()
                .spec(spec)
                .body(payload)
                .when()
                .post(BASE_PATH + "/" + ownerType + "/" + ownerId)
                .then()
                .extract()
                .response();
    }

    public Response getAvatar(String ownerType, Object ownerId) {
        return getAvatar(RequestSpecFactory.authenticatedSpec(), ownerType, ownerId);
    }

    @Step("Fetch image asset for {ownerType} {ownerId}")
    public Response getAvatar(RequestSpecification spec, String ownerType, Object ownerId) {
        return given()
                .spec(spec)
                .when()
                .get(BASE_PATH + "/" + ownerType + "/" + ownerId)
                .then()
                .extract()
                .response();
    }
}
