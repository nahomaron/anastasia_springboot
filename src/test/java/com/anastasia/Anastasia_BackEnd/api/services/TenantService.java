package com.anastasia.Anastasia_BackEnd.api.services;

import com.anastasia.Anastasia_BackEnd.api.config.RequestSpecFactory;
import com.anastasia.Anastasia_BackEnd.model.sms.PhoneVerificationRequest;
import com.anastasia.Anastasia_BackEnd.model.sms.ResendOtpRequest;
import com.anastasia.Anastasia_BackEnd.model.tenant.TenantDTO;
import io.qameta.allure.Step;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

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

    @Step("Verify phone {request.phone}")
    public Response verifyPhone(PhoneVerificationRequest request) {
        return given()
                .spec(RequestSpecFactory.anonymousSpec())
                .body(request)
                .when()
                .post(BASE_PATH + "/verify-phone")
                .then()
                .extract()
                .response();
    }

    @Step("Resend OTP to {request.phone}")
    public Response resendOtp(ResendOtpRequest request) {
        return given()
                .spec(RequestSpecFactory.anonymousSpec())
                .body(request)
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
