package com.anastasia.Anastasia_BackEnd.Api.services;

import com.anastasia.Anastasia_BackEnd.Api.config.ConfigManager;
import com.anastasia.Anastasia_BackEnd.Api.config.RequestSpecFactory;
import com.anastasia.Anastasia_BackEnd.Api.base.BaseApiTest;
import com.anastasia.Anastasia_BackEnd.Api.utils.SchemaValidator;
import com.anastasia.Anastasia_BackEnd.core.auth.dto.AuthenticationRequest;
import com.anastasia.Anastasia_BackEnd.core.auth.dto.AuthenticationResponse;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserDTO;
import io.qameta.allure.Allure;
import io.qameta.allure.Step;
import io.restassured.builder.ResponseBuilder;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.slf4j.Logger;


import java.io.ByteArrayInputStream;
import java.util.Map;

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
        return given()
                .spec(RequestSpecFactory.anonymousSpec())
                .body(request)
                .when()
                .post(ConfigManager.get("auth.signup.endpoint"))
                .then()
                .extract()
                .response();
    }

    @Step("Activate account with token")
    public Response activateAccount(String token, String email) {
        return given()
                .spec(RequestSpecFactory.anonymousSpec())
                .queryParam("token", token)
                .queryParam("email", email)
                .when()
                .get(ConfigManager.get("auth.activate.endpoint"))
                .then()
                .extract()
                .response();
    }

    @Step("Login with email: {request.email}")
    public Response login(AuthenticationRequest request){
        try {
            Response rawResponse = given()
                    .spec(RequestSpecFactory.anonymousSpec())
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


    @Step("Logout user")
    public Response logout(String accessToken) {
        return given()
                .spec(RequestSpecFactory.specWithHeaders(
                        Map.of("Authorization", "Bearer " + accessToken)))
                .when()
                .post("/auth/logout")
                .then()
                .extract()
                .response();
    }


    @Step("Login and extract token for email: {request.email}")
    public AuthenticationResponse loginAndExtractToken(AuthenticationRequest request) {
        Response response = login(request);
        SchemaValidator.validate(response);

        if (response.statusCode() == 200 && !response.asString().isEmpty()) {
            AuthenticationResponse authResponse = response.as(AuthenticationResponse.class);
            if (authResponse.getRefreshToken() == null || authResponse.getRefreshToken().isBlank()) {
                try {
                    authResponse.setRefreshToken(fetchRefreshTokenForEmail(request.getEmail()));
                } catch (Exception e) {
                    log.warn("Unable to fetch refresh token for {}: {}", request.getEmail(), e.getMessage());
                }
            }
            return authResponse;
        } else {
            System.out.println("Login failed (status " + response.statusCode() + "): " + response.asString());
            return null;
        }

    }

    private String fetchRefreshTokenForEmail(String email) {
        String endpoint = ConfigManager.get("test.refresh.endpoint");
        if (!hasText(endpoint)) {
            endpoint = "/auth/test/refresh-token";
        }
        Response response = given()
                .spec(RequestSpecFactory.testHelperSpec())
                .queryParam("email", email)
                .get(endpoint)
                .then()
                .statusCode(200)
                .extract()
                .response();
        return response.asString().trim();
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }


    public static Response buildFailureResponse(Exception e) {
        int code = (e.getMessage() != null && e.getMessage().contains("Connection refused")) ? 503 : 401;
        ResponseBuilder rb = new ResponseBuilder();
        rb.setStatusCode(code);
        rb.setContentType(ContentType.JSON);
        rb.setBody("{\"message\":\"" + sanitize(e.getMessage()) + "\"}");
        return rb.build();
    }

    private static String sanitize(String input) {
        return input == null ? "Unknown error" : input.replace("\"", "'");
    }


    private void attachResponse(String title, Response response) {
        BaseApiTest.attachJsonIfTestRunning(title, response.asPrettyString(), ".json");
    }


}
