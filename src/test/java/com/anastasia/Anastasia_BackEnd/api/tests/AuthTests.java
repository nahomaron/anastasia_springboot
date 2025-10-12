package com.anastasia.Anastasia_BackEnd.api.tests;

import com.anastasia.Anastasia_BackEnd.api.base.BaseApiTest;
import com.anastasia.Anastasia_BackEnd.api.services.AuthService;
import com.anastasia.Anastasia_BackEnd.model.auth.AuthenticationRequest;
import com.anastasia.Anastasia_BackEnd.model.auth.AuthenticationResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class AuthTests extends BaseApiTest {
    private static String TEST_EMAIL;
    private static final String TEST_PASSWORD = "Password@123";

    public final AuthService authService = new AuthService();

    @BeforeAll
    static void init() {
        TEST_EMAIL = "auto_" + System.currentTimeMillis() + "@mail.com";
        // sign up and activate once for all tests
        AuthFlowHelper.signUpAndActivateAndLogin(TEST_EMAIL);
    }

    @Test
    void shouldLoginSuccessfully(){
        AuthenticationRequest request = new AuthenticationRequest();
        request.setEmail(TEST_EMAIL);
        request.setPassword(TEST_PASSWORD);
        Response res = authService.login(request);

        Assertions.assertEquals(200, res.getStatusCode());
        Assertions.assertTrue(res.asString().contains("access_token"));
    }

    @Test
    void shouldExtractTokenFromResponse() {
        AuthenticationRequest req = new
                AuthenticationRequest(TEST_EMAIL, TEST_PASSWORD);
        AuthenticationResponse authRes = authService.loginAndExtractToken(req);

        Assertions.assertNotNull(authRes, "AuthResponse should not be null");
        Assertions.assertNotNull(authRes.getAccessToken(), "Access token should not be null");

//        System.out.println("JWT Token: " + authRes.getAccessToken());
    }

    @Test
    void shouldFailLoginWithInvalidCredentials(){
        AuthenticationRequest request = new AuthenticationRequest();
        request.setEmail("invalidUser@gmail.com");
        request.setPassword("wrongPassword");
        Response res = authService.login(request);

//        System.out.println("Response status: " + res.statusCode());
//        System.out.println("Response body: " + res.asString());

        Assertions.assertEquals(401, res.getStatusCode());
    }
}
