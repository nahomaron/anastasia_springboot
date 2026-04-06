package com.anastasia.Anastasia_BackEnd.Api.services;

import com.anastasia.Anastasia_BackEnd.Api.config.RequestSpecFactory;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantDTO;
import io.qameta.allure.Step;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;

public class TenantService {

    private static final String BASE_PATH = "/tenant";

    @Step("Subscribe tenant {request.email}")
    public Response subscribeTenant(TenantDTO request) {
        return given()
                .spec(RequestSpecFactory.anonymousSpec())
                .body(request)
                .when()
                .post(BASE_PATH + "/subscription")
                .then()
                .extract()
                .response();
    }

    @Step("Verify phone")
    public Response verifyPhone(String phone, String otp) {
        return given()
                .spec(RequestSpecFactory.anonymousSpec())
                .body(Map.of("phone", phone, "otp", otp))
                .when()
                .post(BASE_PATH + "/verify-phone")
                .then()
                .extract()
                .response();
    }

    @Step("Resend OTP")
    public Response resendOtp(String phone) {
        return given()
                .spec(RequestSpecFactory.anonymousSpec())
                .body(phone == null ? Map.of() : Map.of("phone", phone))
                .when()
                .post(BASE_PATH + "/resend-phone-otp")
                .then()
                .extract()
                .response();
    }

    public Response listTenants(RequestSpecification spec) {
        return given()
                .spec(spec)
                .when()
                .get(BASE_PATH)
                .then()
                .extract()
                .response();
    }

    public Response getTenant(RequestSpecification spec, UUID tenantId) {
        return given()
                .spec(spec)
                .when()
                .get(BASE_PATH + "/" + tenantId)
                .then()
                .extract()
                .response();
    }

    public Response unsubscribeTenant(RequestSpecification spec, UUID tenantId) {
        return given()
                .spec(spec)
                .when()
                .post(BASE_PATH + "/unsubscribe/" + tenantId)
                .then()
                .extract()
                .response();
    }

    public Response updateTenant(RequestSpecification spec, UUID tenantId, TenantDTO payload) {
        return given()
                .spec(spec)
                .body(payload)
                .when()
                .patch(BASE_PATH + "/update/" + tenantId)
                .then()
                .extract()
                .response();
    }

}
