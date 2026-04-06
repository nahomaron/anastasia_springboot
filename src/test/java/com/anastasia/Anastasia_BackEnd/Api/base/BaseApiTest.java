package com.anastasia.Anastasia_BackEnd.Api.base;

import com.anastasia.Anastasia_BackEnd.Api.config.ApiInterceptor;
import com.anastasia.Anastasia_BackEnd.Api.config.ConfigManager;
import com.anastasia.Anastasia_BackEnd.Api.extensions.TestFailureWatcher;
import com.anastasia.Anastasia_BackEnd.Api.flows.SubscriptionFlowHelper;
import com.anastasia.Anastasia_BackEnd.Api.services.AuthService;
import com.anastasia.Anastasia_BackEnd.Api.flows.AuthFlowHelper;
import com.anastasia.Anastasia_BackEnd.Api.utils.DataGenerator;
import com.anastasia.Anastasia_BackEnd.Api.utils.RoleContextFactory;
import com.anastasia.Anastasia_BackEnd.Api.utils.TestDataManager;
import com.anastasia.Anastasia_BackEnd.core.auth.dto.AuthenticationRequest;
import com.anastasia.Anastasia_BackEnd.core.auth.dto.AuthenticationResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.Allure;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.parsing.Parser;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URI;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
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

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final AuthService AUTH_SERVICE = new AuthService();

    static {
        if (System.getProperty("environment") == null) {
            String profiles = System.getProperty("spring.profiles.active");
            if (profiles != null && profiles.contains("api")) {
                System.setProperty("environment", "api");
            } else {
                System.setProperty("environment", "test");
            }
        }
    }

    protected static AuthenticationResponse cachedAuth;
    protected static RequestSpecification authSpec;

    private static String ownerAccessToken;
    private static UUID ownerUserId;
    private static TenantDTO cachedTenant;
    private static UUID cachedTenantId;
    protected static String cachedAccessToken;
    protected static String cachedRefreshToken;
    protected static String cachedEmail;
    protected static String cachedPassword;
    protected static UUID cachedUserId;

    protected static Map<Long, AuthenticationResponse> tokenCache = new HashMap<>();


    // -----------------------------------------------------
    //  Lifecycle Hooks
    // -----------------------------------------------------

    /**
     * One-time suite setup to configure RestAssured and create a seed OWNER tenant.
     * Aborts the suite if the backend is not reachable.
     */
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
        ownerUserId = ownerResult.authResponse() != null && ownerResult.authResponse().getSession() != null
                ? ownerResult.authResponse().getSession().getUserId()
                : null;
        cachedTenant = ownerResult.tenantRequest(); // optional if you need tenant info
        if (ownerResult.authResponse() != null && ownerResult.authResponse().getSession() != null) {
            cachedTenantId = ownerResult.authResponse().getSession().getTenantId();
        } else if (ownerAccessToken != null) {
            cachedTenantId = extractTenantIdFromToken(ownerAccessToken);
        } else {
            cachedTenantId = null;
        }
        log.info("Seed OWNER tenant created: {}", cachedTenant.getOwnerName());
    }

    /**
     * Per-test setup to configure RestAssured and ensure authentication cache is initialized.
     */
    @BeforeEach
    public void configureRestAssured() {
        // 1. Configure RestAssured to talk to the externalised backend
        configureRestAssuredBase();
        RestAssured.defaultParser = Parser.JSON;
        RestAssured.filters(new ApiInterceptor());

        // Write environment info to Allure once
        // (could be optimized to only run once per suite if needed)
        writeEnvironmentInfo();

        // 2. Ensure the expensive one-time setup runs *after* RestAssured is configured
        if (cachedAuth == null) {
            initializeAuthenticationCache();
        }
    }

    /**
     * Per-test teardown to clean up test data on failure.
     * @param testInfo Information about the current test.
     * @param reporter Test reporter for logging.
     */
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
    //  Auth Initialization Logic
    // -----------------------------------------------------

    /**
     * Configures RestAssured's base URI, port, and base path based on resolved base URL.
     */
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

    /**
     * Writes environment info to Allure results for better context in reports.
     * This includes base URL, profile, and generation timestamp.
     */
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

    /**
     * Resolves the base URL for the API from environment variables, system properties,
     * or configuration.
     * @return The resolved base URL.
     * @throws IllegalStateException if no base URL is configured.
     */
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

    /**
     * Checks if a string has non-whitespace text.
     * @param value The string to check.
     * @return True if the string has text, false otherwise.
     */
    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /**
     * Initializes the authentication cache by creating a new user, activating the account,
     * and logging in to obtain tokens.
     * This method is expensive and should be called sparingly.
     */
    protected static void initializeAuthenticationCache() {
        String email = DataGenerator.randomEmail();
        String password = DataGenerator.randomPassword();
        cachedEmail = email;
        cachedPassword = password;

        AuthenticationResponse loginResponse = AuthFlowHelper
                .signUpAndActivateAndLogin(email, password);

        cacheAuthentication(loginResponse);
    }

    private static void cacheAuthentication(AuthenticationResponse authenticationResponse) {
        cachedAuth = authenticationResponse;
        cachedUserId = authenticationResponse.getSession() != null
                ? authenticationResponse.getSession().getUserId()
                : null;
        cachedAccessToken = authenticationResponse.getAccessToken();
        cachedRefreshToken = authenticationResponse.getRefreshToken();
        ensureTenantIdFromToken(cachedAccessToken);

        authSpec = new RequestSpecBuilder()
                .setContentType("application/json")
                .addHeader("Authorization", "Bearer " + cachedAuth.getAccessToken())
                .build();
    }


    /**
     * Utility to get the authenticated spec from any test, renewing the token if expired.
     * @return The authenticated RequestSpecification.
     */
    public static RequestSpecification getAuthenticatedSpec() {
        ensureAuthenticated();
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

    public static String getOwnerAccessToken() {
        return ownerAccessToken;
    }

    public static UUID getOwnerUserId() {
        return ownerUserId;
    }

    public static String getCachedAccessToken() {
        return cachedAccessToken;
    }

    public static String getCachedRefreshToken() {
        return cachedRefreshToken;
    }

    public static String getCachedEmail() {
        return cachedEmail;
    }

    public static String getCachedPassword() {
        return cachedPassword;
    }

    public static UUID getCachedUserId() {
        return cachedUserId;
    }

    public static UUID getCachedTenantId() {
        return cachedTenantId;
    }

    private static void ensureTenantIdFromToken(String token) {
        if (cachedTenantId != null || token == null || token.isBlank()) {
            return;
        }
        cachedTenantId = extractTenantIdFromToken(token);
    }

    private static UUID extractTenantIdFromToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                return null;
            }
            byte[] decoded = Base64.getUrlDecoder().decode(parts[1]);
            String payload = new String(decoded, StandardCharsets.UTF_8);
            JsonNode claims = OBJECT_MAPPER.readTree(payload);
            String tenantIdStr = claims.path("tenantId").asText(null);
            return tenantIdStr == null || tenantIdStr.isBlank() ? null : UUID.fromString(tenantIdStr);
        } catch (Exception ex) {
            log.warn("Failed to extract tenantId from token: {}", ex.getMessage());
            return null;
        }
    }

    /**
     * Ensures that the cached authentication is valid, refreshing it if necessary.
     * Logs the duration of the refresh operation to Allure and console.
     */
    public static void ensureAuthenticated() {
        if (hasValidToken() && !isTokenExpiredIgnoringSignature(cachedAccessToken)) {
            return;
        }

        long start = System.currentTimeMillis();
        String initStage = "login";
        AuthenticationResponse loginResponse = null;
        if (hasText(cachedEmail) && hasText(cachedPassword)) {
            loginResponse = AUTH_SERVICE.loginAndExtractToken(
                    new AuthenticationRequest(cachedEmail, cachedPassword));
        }

        if (loginResponse != null) {
            cacheAuthentication(loginResponse);
        } else {
            initStage = "signup";
            initializeAuthenticationCache();
        }

        long duration = System.currentTimeMillis() - start;
        String message = String.format("Initialized new authentication cache for email: %s via %s (took %d ms)",
                cachedEmail, initStage, duration);

        System.out.println("[Auth Refresh] " + message);

        Allure.addAttachment(
                "Auth Cache Refreshed",
                "text/plain",
                new ByteArrayInputStream(message.getBytes()),
                ".txt"
        );
    }

    public static void ensureOwnerAuthenticated() {
        if (hasText(ownerAccessToken) && !isTokenExpiredIgnoringSignature(ownerAccessToken)) {
            return;
        }

        if (cachedTenant == null || !hasText(cachedTenant.getOwnerEmail()) || !hasText(cachedTenant.getPassword())) {
            throw new IllegalStateException("Owner tenant bootstrap is not initialized");
        }

        AuthenticationResponse loginResponse = AUTH_SERVICE.loginAndExtractToken(
                new AuthenticationRequest(cachedTenant.getOwnerEmail(), cachedTenant.getPassword()));

        if (loginResponse == null || !hasText(loginResponse.getAccessToken())) {
            throw new IllegalStateException("Failed to refresh owner authentication");
        }

        ownerAccessToken = loginResponse.getAccessToken();
        ownerUserId = loginResponse.getSession() != null
                ? loginResponse.getSession().getUserId()
                : ownerUserId;
        cachedTenantId = loginResponse.getSession() != null
                ? loginResponse.getSession().getTenantId()
                : extractTenantIdFromToken(ownerAccessToken);
    }

    private static boolean isTokenExpiredIgnoringSignature(String token) {
        if (!hasText(token)) {
            return true;
        }
        String[] parts = token.split("\\.");
        if (parts.length < 2) {
            return true;
        }
        try {
            byte[] payload = Base64.getUrlDecoder().decode(parts[1]);
            JsonNode claims = OBJECT_MAPPER.readTree(payload);
            long exp = claims.path("exp").asLong(-1);
            if (exp <= 0) {
                return true;
            }
            return Instant.ofEpochSecond(exp).isBefore(Instant.now());
        } catch (Exception ex) {
            log.warn("Unable to decode token expiration: {}", ex.getMessage());
            return true;
        }
    }


}
