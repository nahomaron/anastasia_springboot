package com.anastasia.Anastasia_BackEnd.api.base;

import com.anastasia.Anastasia_BackEnd.api.config.ApiInterceptor;
import com.anastasia.Anastasia_BackEnd.api.config.ConfigManager;
import com.anastasia.Anastasia_BackEnd.api.extensions.TestFailureWatcher;
import com.anastasia.Anastasia_BackEnd.api.flows.SubscriptionFlowHelper;
import com.anastasia.Anastasia_BackEnd.api.services.AuthService;
import com.anastasia.Anastasia_BackEnd.api.flows.AuthFlowHelper;
import com.anastasia.Anastasia_BackEnd.api.utils.DataGenerator;
import com.anastasia.Anastasia_BackEnd.api.utils.RoleContextFactory;
import com.anastasia.Anastasia_BackEnd.api.utils.RoleSeeder;
import com.anastasia.Anastasia_BackEnd.api.utils.TestDataManager;
import com.anastasia.Anastasia_BackEnd.model.auth.AuthenticationRequest;
import com.anastasia.Anastasia_BackEnd.model.auth.AuthenticationResponse;
import com.anastasia.Anastasia_BackEnd.model.role.Role;
import com.anastasia.Anastasia_BackEnd.model.tenant.TenantDTO;
import com.anastasia.Anastasia_BackEnd.util.JwtUtil;
import io.qameta.allure.Allure;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.parsing.Parser;
import io.restassured.specification.RequestSpecification;
import lombok.Getter;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.net.URI;
import java.util.UUID;

/**
 * Base class for API integration tests.
 * Treats the backend as an already-running black-box service and only configures RestAssured
 * before delegating to helper flows.
 */

@Epic("API Tests")
@Feature("External REST Layer")
@ExtendWith(TestFailureWatcher.class)
public class BaseApiTest {

    private static final Logger log = LoggerFactory.getLogger(BaseApiTest.class);

    private static final JwtUtil jwtUtil = new JwtUtil();
    // The static AuthService field was removed as it was not being initialized by Spring (it was null).

    static {
        if (System.getProperty("environment") == null) {
            System.setProperty("environment", "test");
        }
    }

    protected static AuthenticationResponse cachedAuth;
    protected static RequestSpecification authSpec;

    @Getter
    private static String ownerAccessToken;
    private static TenantDTO cachedTenant;
    @Getter
    protected static String cachedAccessToken;
    @Getter
    protected static String cachedRefreshToken;
    @Getter
    protected static String cachedEmail;
    @Getter
    protected static String cachedPassword;

    protected static Map<Long, AuthenticationResponse> tokenCache = new HashMap<>();


    // -----------------------------------------------------
    //  Lifecycle Hooks
    // -----------------------------------------------------
    @BeforeAll
    static void beforeAllSuite() {
        log.info("----- Starting API Test Suite -----");
        configureRestAssuredBase();
        TestDataManager.resetAllTestData();

        String baseUrl = System.getProperty("base.url", "http://localhost:8080");
        try {
            RestAssured.baseURI = baseUrl;
            RestAssured.given()
                    .basePath("")
                    .get("/actuator/health")
                    .then()
                    .statusCode(200);
        } catch (Exception e) {
            Assumptions.abort("Backend not reachable at " + baseUrl + ". Skipping black-box tests.");
        }

        // Subscribe a new tenant (this will create an OWNER)
        SubscriptionFlowHelper.SubscriptionResult ownerResult =
                SubscriptionFlowHelper.subscribeTenantAndLoginOwner();

        ownerAccessToken = ownerResult.accessToken();
        cachedTenant = ownerResult.tenantRequest(); // optional if you need tenant info
        log.info("Seed OWNER tenant created: {}", cachedTenant.getOwnerName());
    }

    @BeforeEach
    public void configureRestAssured() {
        // 1. Configure RestAssured to talk to the externalised backend
        configureRestAssuredBase();
        RestAssured.defaultParser = Parser.JSON;
        RestAssured.filters(new ApiInterceptor());

        writeEnvironmentInfo();

        // 2. Ensure the expensive one-time setup runs *after* RestAssured is configured
        if (cachedAuth == null) {
            initializeAuthenticationCache();
        }
    }

    @AfterEach
    void afterEachTest(TestInfo testInfo, TestReporter reporter) {
        boolean testFailed = testInfo.getTags().contains("failed"); // fallback, depends on how you mark failures
        if (testFailed) {
            TestDataManager.cleanupOnFailure(cachedEmail, true);
        }
    }

    @AfterAll
    static void afterAllSuite() {
        log.info("----- API Test Suite Finished. Exporting cleanup summary -----");
        TestDataManager.exportSummaryToAllure();
    }

    // -----------------------------------------------------
    // Existing Auth Initialization Logic
    // -----------------------------------------------------

    private static void configureRestAssuredBase() {
        String configuredBaseUrl = resolveBaseUrl();

        URI uri = URI.create(configuredBaseUrl);
        String scheme = uri.getScheme() != null ? uri.getScheme() : "http";
        String host = uri.getHost() != null ? uri.getHost() : "localhost";

        RestAssured.baseURI = scheme + "://" + host;

        if (uri.getPort() > 0) {
            RestAssured.port = uri.getPort();
        } else {
            RestAssured.port = "https".equalsIgnoreCase(scheme) ? 443 : 80;
        }

        String path = uri.getPath();
        if (path != null && !path.isBlank() && !"/".equals(path)) {
            RestAssured.basePath = path;
        } else {
            RestAssured.basePath = "/api/v1";
        }
    }

    private static void writeEnvironmentInfo() {
        try {
            Path allureResults = Path.of("target/allure-results");
            Files.createDirectories(allureResults);
            Properties props = new Properties();
            props.setProperty("Environment", "Local Test");
            props.setProperty("BaseURL", resolveBaseUrl());
            props.setProperty("Profile", ConfigManager.getEnvironment());
            props.setProperty("GeneratedAt", java.time.LocalDateTime.now().toString());
            try (FileOutputStream fos = new FileOutputStream(allureResults.resolve("environment.properties").toFile())) {
                props.store(fos, "Allure Environment Info");
            }
        } catch (IOException e) {
            System.err.println("Failed to write environment info: " + e.getMessage());
        }
    }

    private static String resolveBaseUrl() {
        String envOverride = System.getenv("BASE_URL");
        if (hasText(envOverride)) {
            return envOverride.trim();
        }

        String systemProperty = System.getProperty("base.url");
        if (hasText(systemProperty)) {
            return systemProperty.trim();
        }

        String configuredBaseUrl = ConfigManager.get("base.url");
        if (hasText(configuredBaseUrl)) {
            return configuredBaseUrl.trim();
        }

        throw new IllegalStateException("Missing base.url configuration for environment "
                + ConfigManager.getEnvironment());
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    protected static void initializeAuthenticationCache() {
        String email = DataGenerator.randomEmail();
        String password = DataGenerator.randomPassword();
        cachedEmail = email;
        cachedPassword = password;

        // Perform the full sign-up -> activate -> login flow.
        AuthenticationResponse loginResponse = AuthFlowHelper.signUpAndActivateAndLogin(email, password);

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


            AuthenticationRequest request = new AuthenticationRequest(email, cachedPassword);

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

    /**
     * Creates a new user, logs them in, caches the token, and returns an authenticated spec.
     */
    public static RequestSpecification getSpecForRole(String role) {
        return RoleContextFactory.getSpecForRole(role);
    }


    public static boolean hasValidToken() {
        return cachedAccessToken != null && !cachedAccessToken.isBlank();
    }

    public static void ensureAuthenticated() {
        boolean needsAuth = (cachedAuth == null || cachedAccessToken == null || cachedAccessToken.isBlank());

        if (!needsAuth && jwtUtil.isTokenExpired(cachedAccessToken)) {
            needsAuth = true;
        }

        if (needsAuth) {
            long start = System.currentTimeMillis();
            initializeAuthenticationCache();
            long duration = System.currentTimeMillis() - start;

            // Write to Allure report and logs
            String message = String.format("Initialized new authentication cache for email: %s (took %d ms)",
                    cachedEmail, duration);

            System.out.println("[Auth Refresh] " + message);

            Allure.addAttachment(
                    "Auth Cache Refreshed",
                    "text/plain",
                    new ByteArrayInputStream(message.getBytes()),
                    ".txt"
            );
        }
    }


}
