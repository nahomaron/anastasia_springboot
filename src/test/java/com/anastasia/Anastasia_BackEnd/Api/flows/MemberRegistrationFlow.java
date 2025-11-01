package com.anastasia.Anastasia_BackEnd.Api.flows;

import com.anastasia.Anastasia_BackEnd.Api.factories.MemberDataFactory;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.MemberDTO;
import io.restassured.response.Response;

import static io.restassured.RestAssured.given;

public final class MemberRegistrationFlow {
    private MemberRegistrationFlow() {}

    public static void registerMember(String email, String password) {
        var login = AuthFlowHelper.signUpAndActivateAndLogin(email, password);
        var token = login.getAccessToken();

        MemberDTO member = MemberDataFactory.newValidMember();

        Response res = given()
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .body(member)
                .post("/registrar/members/register-member")
                .then()
                .extract().response();

        if (res.statusCode() != 201) {
            throw new RuntimeException("Failed to register member: " + res.statusCode() + " " + res.asString());
        }
    }
}
