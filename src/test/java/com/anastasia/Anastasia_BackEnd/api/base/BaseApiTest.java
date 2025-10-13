package com.anastasia.Anastasia_BackEnd.api.base;

import com.anastasia.Anastasia_BackEnd.api.config.ApiInterceptor;
import com.anastasia.Anastasia_BackEnd.api.services.AuthService;
import com.anastasia.Anastasia_BackEnd.api.tests.AuthFlowHelper;
import com.anastasia.Anastasia_BackEnd.model.auth.AuthenticationRequest;
import com.anastasia.Anastasia_BackEnd.model.auth.AuthenticationResponse;
import com.anastasia.Anastasia_BackEnd.util.JwtUtil;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.parsing.Parser;
import io.restassured.specification.RequestSpecification;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.Map;

/**
 * Base class for API integration tests.
 * Starts the application on a random port for isolation, ensuring the web environment is fully started
 * before tests attempt to connect via RestAssured.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@RequiredArgsConstructor
public class BaseApiTest {

    private static final JwtUtil jwtUtil = new JwtUtil();
    // The static AuthService field was removed as it was not being initialized by Spring (it was null).

    protected static AuthenticationResponse cachedAuth;
    protected static RequestSpecification authSpec;

    protected static String cachedAccessToken;
    protected static String cachedRefreshToken;
    protected static String cachedEmail;

    // Spring injects the actual random port the server started on
    @LocalServerPort
    private int port; // Must be an instance field

    // --- Setup Logic ---

    /**
     * Configures RestAssured with the dynamic port and ensures the authentication
     * cache is initialized before any test method runs.
     * This method runs once per test class instance (i.e., before each test method).
     */
    @BeforeEach
    public void configureRestAssured() {
        // 1. Set the base URI and port using the injected fields
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = this.port;
        RestAssured.basePath = "/api/v1";
        RestAssured.defaultParser = Parser.JSON;
        RestAssured.filters(new ApiInterceptor());

        // 2. Ensure the expensive one-time setup runs *after* RestAssured is configured
        if (cachedAuth == null) {
            initializeAuthenticationCache();
        }
    }

    protected static void initializeAuthenticationCache() {
        String email = "api_user_" + System.currentTimeMillis() + "@mail.com";
        cachedEmail = email;

        // Perform the full sign-up -> activate -> login flow.
        AuthenticationResponse loginResponse = AuthFlowHelper.signUpAndActivateAndLogin(email);

        cachedAuth = loginResponse;
        cachedAccessToken = loginResponse.getAccessToken();
        cachedRefreshToken = loginResponse.getRefreshToken();

        // Build the authenticated spec
        authSpec = new RequestSpecBuilder()
                .setContentType("application/json")
                .addHeader("Authorization", "Bearer " + cachedAuth.getAccessToken())
                .build();
    }

    // --- Utility Methods ---

    /**
     * Utility to get the authenticated spec from any test, renewing the token if expired.
     */
    public static RequestSpecification getAuthenticatedSpec() {
        if (cachedAuth == null) {
            // Should not happen if @BeforeEach runs, but as a safeguard:
            initializeAuthenticationCache();
        }

        if (jwtUtil.isTokenExpired(cachedAuth.getAccessToken())) {
            String email = jwtUtil.extractUsername(cachedAuth.getAccessToken());

            AuthenticationRequest request = new AuthenticationRequest(email, "Password@123");

            // FIX: Create local instance of AuthService as the static field was null
            AuthService localAuthService = new AuthService();

            AuthenticationResponse renewedAuth = localAuthService.loginAndExtractToken(request);
            if (renewedAuth != null) {
                cachedAuth = renewedAuth;
            }

            // Rebuild spec with new token
            authSpec = new RequestSpecBuilder()
                    .setContentType("application/json")
                    .addHeader("Authorization", "Bearer " + cachedAuth.getAccessToken())
                    .build();
        }

        return authSpec;
    }

    /** Helper to add Authorization header automatically */
    protected String bearerToken() {
        return "Bearer " + cachedAccessToken;
    }

    protected static Map<String, AuthenticationResponse> tokenCache = new HashMap<>();

    /**
     * Creates a new user, logs them in, caches the token, and returns an authenticated spec.
     */
    public static RequestSpecification getSpecForRole(String role) {
        if (!tokenCache.containsKey(role)) {
            tokenCache.put(role, AuthFlowHelper.signUpAndActivateAndLogin(role + "@mail.com"));
        }
        return new RequestSpecBuilder()
                .setContentType("application/json")
                .addHeader("Authorization", "Bearer " + tokenCache.get(role).getAccessToken())
                .build();
    }
}
