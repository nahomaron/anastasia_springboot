package com.anastasia.Anastasia_BackEnd.api.tests;

import com.anastasia.Anastasia_BackEnd.api.config.ConfigManager;
import com.anastasia.Anastasia_BackEnd.api.services.AuthService;
import com.anastasia.Anastasia_BackEnd.model.auth.AuthenticationRequest;
import com.anastasia.Anastasia_BackEnd.model.auth.AuthenticationResponse;
import com.anastasia.Anastasia_BackEnd.model.user.UserDTO;
import io.qameta.allure.Step;
import io.restassured.response.Response;
import org.junit.jupiter.api.Assertions;

import static io.restassured.RestAssured.given;

public class AuthFlowHelper {
    private static final AuthService authService = new AuthService();

    public static AuthenticationResponse signUpAndActivateAndLogin(String email) {
        String password = "Password@123";

        // 1️⃣ Sign up
        UserDTO signUp = UserDTO.builder()
                .fullName("Test User")
                .email(email)
                .password(password)
                .confirmPassword(password)
                .build();

        Response signUpRes = authService.signUp(signUp);
        Assertions.assertEquals(201, signUpRes.statusCode(), "Signup failed");

        // 2️⃣ Fetch activation token (from DB or test endpoint)
        Response tokenRes = given()
                .queryParam("email", email)
                .get(ConfigManager.get("test.activation.endpoint"))
                .then().extract().response();

        String token = tokenRes.asString();

        // 3️⃣ Activate account
        Response activateRes = authService.activateAccount(token);
        Assertions.assertEquals(200, activateRes.statusCode(), "Activation failed");

        // 4️⃣ Login
        AuthenticationRequest loginReq = new AuthenticationRequest(email, password);
        AuthenticationResponse loginRes = authService.loginAndExtractToken(loginReq);

        Assertions.assertNotNull(loginRes.getAccessToken(), "Token must not be null");
        return loginRes;
    }
}

