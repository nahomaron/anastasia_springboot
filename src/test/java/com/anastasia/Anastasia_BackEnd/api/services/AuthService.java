package com.anastasia.Anastasia_BackEnd.api.services;

import com.anastasia.Anastasia_BackEnd.api.config.ConfigManager;
import com.anastasia.Anastasia_BackEnd.model.auth.AuthenticationRequest;
import com.anastasia.Anastasia_BackEnd.model.auth.AuthenticationResponse;
import com.anastasia.Anastasia_BackEnd.model.user.UserDTO;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

import static io.restassured.RestAssured.given;

/**
    * Service class to handle authentication-related API calls.
    * Provides methods to perform login and extract authentication tokens.
    * Uses RestAssured for making HTTP requests.
 */
public class AuthService {

    public Response signUp(UserDTO request) {
        return given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post(ConfigManager.get("auth.signup.endpoint"))
                .then()
                .log().ifValidationFails()
                .extract()
                .response();
    }

    public Response activateAccount(String token) {
        return given()
                .when()
                .get(ConfigManager.get("auth.activate.endpoint") + "?token=" + token)
                .then()
                .log().ifValidationFails()
                .extract()
                .response();
    }

    public Response login(AuthenticationRequest request){
        return given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
//                .log().body()
                .body(request)
                .when()
                .post(ConfigManager.get("auth.login.endpoint"))
                .then()
//                .log().ifValidationFails()
                .extract()
                .response();
    }

    public AuthenticationResponse loginAndExtractToken(AuthenticationRequest request) {
        Response response = login(request);

        if (response.statusCode() == 200 && !response.asString().isEmpty()) {
            return response.as(AuthenticationResponse.class);
        } else {
            System.out.println("Login failed (status " + response.statusCode() + "): " + response.asString());
            return null;
        }
    }
}
