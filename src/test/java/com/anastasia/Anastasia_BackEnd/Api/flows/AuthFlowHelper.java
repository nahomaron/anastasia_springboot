package com.anastasia.Anastasia_BackEnd.Api.flows;

import com.anastasia.Anastasia_BackEnd.Api.config.ConfigManager;
import com.anastasia.Anastasia_BackEnd.Api.config.RequestSpecFactory;
import com.anastasia.Anastasia_BackEnd.Api.services.AuthService;
import com.anastasia.Anastasia_BackEnd.Api.utils.DataGenerator;
import com.anastasia.Anastasia_BackEnd.core.auth.dto.AuthenticationRequest;
import com.anastasia.Anastasia_BackEnd.core.auth.dto.AuthenticationResponse;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserDTO;
import io.restassured.response.Response;
import org.junit.jupiter.api.Assertions;

import static io.restassured.RestAssured.given;

public class AuthFlowHelper {
    private static final AuthService authService = new AuthService();

    private static final String DEFAULT_TEST_ACTIVATION_ENDPOINT = "/auth/test/activation-token";

    public static AuthenticationResponse signUpAndActivateAndLogin(String email, String password) {
        // 1️⃣ Sign up
        UserDTO signUp = UserDTO.builder()
                .fullName(DataGenerator.randomName())
                .email(email)
                .password(password)
                .confirmPassword(password)
                .build();

        Response signUpRes = authService.signUp(signUp);
        Assertions.assertEquals(201, signUpRes.statusCode(), "Signup failed");

        // 2️⃣ Fetch activation token (from DB or test endpoint)
        String token = fetchActivationToken(email);

        // 3️⃣ Activate account
        Response activateRes = authService.activateAccount(token, email);
        Assertions.assertEquals(200, activateRes.statusCode(), "Activation failed");

        // 4️⃣ Login
        AuthenticationRequest loginReq = new AuthenticationRequest(email, password);
        AuthenticationResponse loginRes = authService.loginAndExtractToken(loginReq);

        Assertions.assertNotNull(loginRes.getAccessToken(), "Token must not be null");
        return loginRes;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static String fetchActivationToken(String email) {
        String endpoint = ConfigManager.get("test.activation.endpoint");
        if (!hasText(endpoint)) {
            endpoint = DEFAULT_TEST_ACTIVATION_ENDPOINT;
        }
        Response response = given()
                .spec(RequestSpecFactory.testHelperSpec())
                .queryParam("email", email)
                .get(endpoint)
                .then()
                .extract()
                .response();

        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to fetch token from " + endpoint + ". Status: "
                    + response.statusCode() + " Body: " + response.asString());
        }

        String token = response.asString();
        Assertions.assertTrue(hasText(token), "Test activation token could not be retrieved for email: " + email);
        return token.trim();
    }
}
