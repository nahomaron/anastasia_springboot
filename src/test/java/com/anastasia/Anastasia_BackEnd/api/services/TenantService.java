package com.anastasia.Anastasia_BackEnd.api.services;

import com.anastasia.Anastasia_BackEnd.api.config.RequestSpecFactory;
import com.anastasia.Anastasia_BackEnd.model.sms.PhoneVerificationRequest;
import com.anastasia.Anastasia_BackEnd.model.tenant.TenantDTO;
import io.qameta.allure.Step;
import io.restassured.response.Response;

import static io.restassured.RestAssured.given;

public class TenantService {

    @Step("Subscribe tenant {request.email}")
    public Response subscribeTenant(TenantDTO request) {
        return given()
                .spec(RequestSpecFactory.anonymousSpec())
                .body(request)
                .when()
                .post("/tenant/subscription")
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
                .post("/tenant/verify-phone")
                .then()
                .extract()
                .response();
    }
}
