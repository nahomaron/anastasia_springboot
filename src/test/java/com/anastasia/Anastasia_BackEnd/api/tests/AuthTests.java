package com.anastasia.Anastasia_BackEnd.api.tests;

import com.anastasia.Anastasia_BackEnd.api.base.BaseApiTest;
import com.anastasia.Anastasia_BackEnd.api.flows.AuthFlowHelper;
import com.anastasia.Anastasia_BackEnd.api.services.AuthService;
import com.anastasia.Anastasia_BackEnd.api.utils.DataGenerator;
import com.anastasia.Anastasia_BackEnd.api.utils.SchemaValidator;
import com.anastasia.Anastasia_BackEnd.api.utils.TestDataManager;
import com.anastasia.Anastasia_BackEnd.model.auth.AuthenticationRequest;
import com.anastasia.Anastasia_BackEnd.model.auth.AuthenticationResponse;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.*;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;

@Epic("Authentication")
@Feature("User Account Management")
@Story("Sign up, activate, and login flow")
@Owner("Nahom Aron")
@Severity(SeverityLevel.CRITICAL)
public class AuthTests extends BaseApiTest {
    private String testEmail;
    private String testPassword;

    public final AuthService authService = new AuthService();

    @BeforeEach
    @Description("Verifies user can sign up, activate, and login successfully.")
    void setupTestUser() {
        System.out.println("Running pre-test cleanup...");
        TestDataManager.resetAllTestData();
        // Create a unique user for these specific login tests
        testEmail = DataGenerator.randomEmail();
        testPassword = DataGenerator.randomPassword();
        // Ensure this user is signed up and activated *before* the login tests run
        AuthFlowHelper.signUpAndActivateAndLogin(testEmail, testPassword);
    }

    @AfterEach
    void cleanup(TestInfo testInfo) {
        boolean testFailed = testInfo.getTags().contains("failed");
        TestDataManager.cleanupOnFailure(testEmail, testFailed);
        TestDataManager.cleanupIfEnabled(testEmail);
    }

    @AfterAll
    static void exportCleanupSummary() {
        TestDataManager.exportSummaryToAllure();
    }

    @Test
    @Feature("Login")
    @Story("User can login with valid credentials")
    void shouldLoginSuccessfully(){
        AuthenticationRequest request = new AuthenticationRequest(testEmail, testPassword);
        Response res = authService.login(request);

        Assertions.assertEquals(200, res.getStatusCode());
        Assertions.assertTrue(res.asString().contains("access_token"));

        SchemaValidator.validate(res);
    }

    @Test
    @Description("Verifies authentication token is extracted from login response.")
    void shouldExtractTokenFromResponse() {
        AuthenticationRequest req = new
                AuthenticationRequest(testEmail, testPassword);
        AuthenticationResponse authRes = authService.loginAndExtractToken(req);

        Assertions.assertNotNull(authRes, "AuthResponse should not be null");
        Assertions.assertNotNull(authRes.getAccessToken(), "Access token should not be null");

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
        Assertions.assertTrue(res.asString().contains("Unauthorized"));

    }
}
