package com.anastasia.Anastasia_BackEnd.api.tests;

import com.anastasia.Anastasia_BackEnd.api.base.BaseApiTest;
import com.anastasia.Anastasia_BackEnd.api.services.AuthService;
import com.anastasia.Anastasia_BackEnd.model.auth.AuthenticationRequest;
import com.anastasia.Anastasia_BackEnd.model.auth.AuthenticationResponse;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.*;
import io.restassured.response.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@Epic("Authentication")
@Feature("User Account Management")
@Story("Sign up, activate, and login flow")
@Owner("Nahom Aron")
@Severity(SeverityLevel.CRITICAL)
public class AuthTests extends BaseApiTest {
//    private static String TEST_EMAIL;
    private String testEmail;
    private static final String TEST_PASSWORD = "Password@123";

    public final AuthService authService = new AuthService();

    @BeforeEach
    @Description("Verifies user can sign up, activate, and login successfully.")
    void setupTestUser() {
        // Create a unique user for these specific login tests
        testEmail = "auth_test_" + System.currentTimeMillis() + "@mail.com";
        // Ensure this user is signed up and activated *before* the login tests run
        AuthFlowHelper.signUpAndActivateAndLogin(testEmail);
    }

    @Test
    @Feature("Login")
    @Story("User can login with valid credentials")
    void shouldLoginSuccessfully(){
        AuthenticationRequest request = new AuthenticationRequest();
        request.setEmail(testEmail);
        request.setPassword(TEST_PASSWORD);
        Response res = authService.login(request);

        Assertions.assertEquals(200, res.getStatusCode());
        Assertions.assertTrue(res.asString().contains("access_token"));
    }

    @Test
    @Description("Verifies authentication token is extracted from login response.")
    void shouldExtractTokenFromResponse() {
        AuthenticationRequest req = new
                AuthenticationRequest(testEmail, TEST_PASSWORD);
        AuthenticationResponse authRes = authService.loginAndExtractToken(req);

        Assertions.assertNotNull(authRes, "AuthResponse should not be null");
        Assertions.assertNotNull(authRes.getAccessToken(), "Access token should not be null");

//        System.out.println("JWT Token: " + authRes.getAccessToken());
    }

    @Test
    @Description("Verifies login fails with invalid credentials.")
    void shouldFailLoginWithInvalidCredentials(){
        authService.logout(BaseApiTest.cachedAccessToken);
        AuthenticationRequest request = new AuthenticationRequest();

        request.setEmail("invalidUser@gmail.com");
        request.setPassword("wrongPassword");
        Response res = authService.login(request);

        Assertions.assertEquals(401, res.getStatusCode());
    }
}
