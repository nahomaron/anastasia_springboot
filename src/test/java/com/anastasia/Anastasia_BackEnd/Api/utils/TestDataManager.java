package com.anastasia.Anastasia_BackEnd.Api.utils;

import com.anastasia.Anastasia_BackEnd.Api.config.ConfigManager;
import io.qameta.allure.Allure;
import io.restassured.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static io.restassured.RestAssured.given;

/**
 * TestDataManager - Utility for managing and cleaning up test data between API test runs.
 * Provides resilient cleanup methods with Allure integration and detailed tracking.
 */
public class TestDataManager {

    private static final Logger log = LoggerFactory.getLogger(TestDataManager.class);

    // --------------------------------------------------------------------
    // Configuration Resolution
    // --------------------------------------------------------------------
    private static final String DEFAULT_CLEANUP_BASE = "/test-utils/cleanup";
    private static final String CLEANUP_BASE = normalizeBase(resolveCleanupBase());
    private static final boolean ABSOLUTE_ENDPOINT = isAbsolute(CLEANUP_BASE);

    private static final boolean CLEANUP_ENABLED =
            Boolean.parseBoolean(System.getenv().getOrDefault("CLEANUP_ON_EXIT",
                    System.getProperty("cleanup.on.exit", "false")));

    private static final boolean CLEANUP_ON_FAILURE_ONLY =
            Boolean.parseBoolean(System.getenv().getOrDefault("CLEANUP_ON_FAILURE_ONLY",
                    System.getProperty("cleanup.on.failure.only", "true")));

    // Thread-safe cleanup summary log for Allure export
    private static final Queue<Map<String, Object>> cleanupSummary = new ConcurrentLinkedQueue<>();


    // --------------------------------------------------------------------
    // Public Cleanup Methods
    // --------------------------------------------------------------------

    public static void deleteUserByEmail(String email) {
        runCleanup("User", email, () ->
                given()
                        .basePath("")
                        .queryParam("email", email)
                        .when()
                        .delete(buildPath(null))
                        .then()
                        .extract()
                        .response()
        );
    }

    public static void deleteTenantByEmail(String email) {
        runCleanup("Tenant", email, () ->
                given()
                        .basePath("")
                        .queryParam("email", email)
                        .when()
                        .delete(buildPath("tenant"))
                        .then()
                        .extract()
                        .response()
        );
    }

    public static void deleteMemberById(String memberId) {
        runCleanup("Member", memberId, () ->
                given()
                        .basePath("")
                        .queryParam("id", memberId)
                        .when()
                        .delete(buildPath("member"))
                        .then()
                        .extract()
                        .response()
        );
    }

    public static void bulkDeleteUsers(List<String> emails) {
        if (emails == null || emails.isEmpty()) return;
        emails.forEach(TestDataManager::deleteUserByEmail);
    }

    public static void resetAllTestData() {
        runCleanup("Global Reset", "All Entities", () ->
                given()
                        .basePath("")
                        .when()
                        .post(buildPath("reset-all"))
                        .then()
                        .extract()
                        .response()
        );
    }


    // --------------------------------------------------------------------
    // Conditional Hooks for CI or Test Failures
    // --------------------------------------------------------------------

    public static void cleanupIfEnabled(String email) {
        if (!CLEANUP_ENABLED) {
            log.info("CLEANUP_ON_EXIT disabled. Skipping cleanup for {}", email);
            return;
        }
        deleteUserByEmail(email);
    }

    public static void cleanupOnFailure(String email, boolean testFailed) {
        if (!testFailed) return;
        if (!CLEANUP_ON_FAILURE_ONLY) {
            log.info("CLEANUP_ON_FAILURE_ONLY disabled. Skipping conditional cleanup.");
            return;
        }
        log.info("Test failed. Running conditional cleanup for {}", email);
        deleteUserByEmail(email);
    }


    // --------------------------------------------------------------------
    // Allure Summary Export
    // --------------------------------------------------------------------

    public static void exportSummaryToAllure() {
        if (cleanupSummary.isEmpty()) {
            log.info("No cleanup actions recorded.");
            return;
        }

        StringBuilder json = new StringBuilder("[\n");
        cleanupSummary.forEach(entry ->
                json.append("  ").append(new LinkedHashMap<>(entry)).append(",\n"));
        json.append("]");

        Allure.addAttachment(
                "Cleanup Summary",
                "application/json",
                new ByteArrayInputStream(json.toString().getBytes(StandardCharsets.UTF_8)),
                ".json"
        );

        log.info("Cleanup Summary exported to Allure:\n{}", json);
    }


    // --------------------------------------------------------------------
    // Internal Core Logic
    // --------------------------------------------------------------------

    private static void runCleanup(String type, String identifier, CleanupAction action) {
        Instant start = Instant.now();
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("entity", type);
        record.put("target", identifier);

        try {
            Response res = action.execute();
            int code = res.statusCode();

            record.put("status", code);
            record.put("success", code >= 200 && code < 300);
            record.put("durationMs", Duration.between(start, Instant.now()).toMillis());
            cleanupSummary.add(record);

            if (code >= 200 && code < 300) {
                log.info("{} cleanup succeeded for {}", type, identifier);
            } else {
                log.error("{} cleanup failed for {} (status: {})", type, identifier, code);
            }

            attachCleanupResponse(type + " Cleanup: " + identifier, res);

        } catch (Exception e) {
            record.put("success", false);
            record.put("error", e.getMessage());
            record.put("durationMs", Duration.between(start, Instant.now()).toMillis());
            cleanupSummary.add(record);

            log.error("Exception during {} cleanup for {}: {}", type, identifier, e.getMessage());
            attachTextAttachment(type + " Cleanup Error (" + identifier + ")", e.toString());
        }
    }

    private static void attachCleanupResponse(String title, Response response) {
        try {
            Allure.addAttachment(
                    title + " (Status: " + response.statusCode() + ")",
                    "application/json",
                    new ByteArrayInputStream(response.asPrettyString().getBytes(StandardCharsets.UTF_8)),
                    ".json"
            );
        } catch (Exception e) {
            log.warn("Failed to attach cleanup response to Allure: {}", e.getMessage());
        }
    }

    private static void attachTextAttachment(String title, String message) {
        try {
            Allure.addAttachment(
                    title,
                    "text/plain",
                    new ByteArrayInputStream(message.getBytes(StandardCharsets.UTF_8)),
                    ".txt"
            );
        } catch (Exception ignored) {}
    }


    // --------------------------------------------------------------------
    // Helper Methods
    // --------------------------------------------------------------------

    private static String resolveCleanupBase() {
        String env = System.getenv("TEST_CLEANUP_ENDPOINT");
        if (hasText(env)) return env.trim();

        String sys = System.getProperty("test.cleanup.endpoint");
        if (hasText(sys)) return sys.trim();

        String configValue = ConfigManager.get("test.cleanup.endpoint");
        if (hasText(configValue)) return configValue.trim();

        return DEFAULT_CLEANUP_BASE;
    }

    private static boolean isAbsolute(String value) {
        return value.startsWith("http://") || value.startsWith("https://");
    }

    private static String normalizeBase(String raw) {
        if (!hasText(raw)) raw = DEFAULT_CLEANUP_BASE;
        String trimmed = raw.trim();

        if (isAbsolute(trimmed)) {
            return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
        }
        if (!trimmed.startsWith("/")) {
            trimmed = "/" + trimmed;
        }
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }

    private static String buildPath(String suffix) {
        if (!hasText(suffix)) return CLEANUP_BASE;
        String normalized = suffix.trim();
        if (normalized.startsWith("/")) normalized = normalized.substring(1);
        return CLEANUP_BASE + "/" + normalized;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    @FunctionalInterface
    private interface CleanupAction {
        Response execute();
    }
}
