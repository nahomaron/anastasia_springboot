package com.anastasia.Anastasia_BackEnd.api.services;

import com.anastasia.Anastasia_BackEnd.api.config.ConfigManager;
import com.anastasia.Anastasia_BackEnd.model.auth.AuthenticationRequest;
import com.anastasia.Anastasia_BackEnd.model.auth.AuthenticationResponse;
import com.anastasia.Anastasia_BackEnd.model.user.UserDTO;
import io.qameta.allure.Step;
import io.restassured.builder.ResponseBuilder;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.slf4j.Logger;


import static io.restassured.RestAssured.given;
    
/**
    * Service class to handle authentication-related API calls.
    * Provides methods to perform login and extract authentication tokens.
    * Uses RestAssured for making HTTP requests.
 */
public class AuthService {
    Logger log = org.slf4j.LoggerFactory.getLogger(AuthService.class);

    @Step("Sign up user with email: {request.email}")
    public Response signUp(UserDTO request) {
        log.info("Signup payload:\n{}", request);

        return given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post(ConfigManager.get("auth.signup.endpoint"))
                .then()
                .extract()
                .response();
    }

    @Step("Activate account with token")
    public Response activateAccount(String token) {
        return given()
                .when()
                .get(ConfigManager.get("auth.activate.endpoint") + "?token=" + token)
                .then()
                .extract()
                .response();
    }

    @Step("Login with email: {request.email}")
    public Response login(AuthenticationRequest request){
        try {
            Response rawResponse = given()
                    .contentType(ContentType.JSON)
                    .accept(ContentType.JSON)
                    .body(request)
                    .when()
                    .post(ConfigManager.get("auth.login.endpoint"));

            if (rawResponse == null) {
                log.error("Login attempt to {} returned a null response object",
                        ConfigManager.get("auth.login.endpoint"));
                return buildFailureResponse(new Exception("Null response from server"));
            }

            return rawResponse.then()
                    .extract()
                    .response();
        } catch (Exception e) {
            // Catching the broad exception (often IO, Connect, or Wrapped)
            log.error("Network or IO error during login attempt to {}: {}",
                    ConfigManager.get("auth.login.endpoint"), e.getMessage(), e);
            return buildFailureResponse(e);
        }
    }


    @Step("Logout with access token")
    public void logout(String accessToken) {
        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .when()
                .post("/auth/logout")
                .then()
                .extract()
                .response();
    }


    @Step("Login and extract token for email: {request.email}")
    public AuthenticationResponse loginAndExtractToken(AuthenticationRequest request) {
        Response response = login(request);

        if (response.statusCode() == 200 && !response.asString().isEmpty()) {
            return response.as(AuthenticationResponse.class);
        } else {
            System.out.println("Login failed (status " + response.statusCode() + "): " + response.asString());
            return null;
        }
    }

    private Response buildFailureResponse(Exception e) {
        int code = (e.getMessage() != null && e.getMessage().contains("Connection refused")) ? 503 : 401;
        ResponseBuilder rb = new ResponseBuilder();
        rb.setStatusCode(code);
        rb.setContentType(ContentType.JSON);
        rb.setBody("{\"message\":\"" + e.getMessage() + "\"}");
        return rb.build();
    }

}
