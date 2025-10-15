package com.anastasia.Anastasia_BackEnd.api.utils;

import com.anastasia.Anastasia_BackEnd.api.config.ConfigManager;
import io.qameta.allure.Allure;
import io.restassured.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static io.restassured.RestAssured.given;

/**
 * Utility for managing and cleaning up test data between runs.
 * Provides resilient cleanup methods that continue even if one fails.
 */
public class TestDataManager {
    private static final Logger log = LoggerFactory.getLogger(TestDataManager.class);

    private static final String CLEANUP_ENDPOINT = ConfigManager.get("test.cleanup.endpoint");

    // ---------- Single Resource Cleanup ---------- //

    public static void deleteUserByEmail(String email) {
        runCleanup("User", email, () ->
                given().queryParam("email", email)
                        .when().delete(CLEANUP_ENDPOINT)
                        .then().extract().response()
        );
    }

    public static void deleteTenantByEmail(String email) {
        runCleanup("Tenant", email, () ->
                given().queryParam("email", email)
                        .when().delete(CLEANUP_ENDPOINT + "/tenant")
                        .then().extract().response()
        );
    }

    public static void deleteMemberById(String memberId) {
        runCleanup("Member", memberId, () ->
                given().queryParam("id", memberId)
                        .when().delete(CLEANUP_ENDPOINT + "/member")
                        .then().extract().response()
        );
    }

    // ---------- Bulk Cleanup ---------- //

    public static void bulkDeleteUsers(List<String> emails) {
        if (emails == null || emails.isEmpty()) return;
        emails.forEach(TestDataManager::deleteUserByEmail);
    }

    // ---------- Full Reset ---------- //

    public static void resetAllTestData() {
        if (CLEANUP_ENDPOINT == null) {
            log.warn("No cleanup endpoint configured.");
            return;
        }

        runCleanup("Global Reset", "All Entities", () ->
                given().when().post(CLEANUP_ENDPOINT + "/reset-all")
                        .then().extract().response()
        );
    }

    // ---------- Internal Helpers ---------- //

    private static void runCleanup(String type, String identifier, CleanupAction action) {
        try {
            if (CLEANUP_ENDPOINT == null) {
                log.warn("No cleanup endpoint for {} {}", type, identifier);
                return;
            }

            Response res = action.execute();
            int code = res.statusCode();

            if (code >= 200 && code < 300) {
                log.info("✔ {} cleanup succeeded for {}", type, identifier);
            } else {
                log.error("❌ {} cleanup failed for {} (status: {})", type, identifier, code);
            }

            attachCleanupResponse(type + " Cleanup: " + identifier, res);

        } catch (Exception e) {
            log.error("⚠ Exception during {} cleanup for {}: {}", type, identifier, e.getMessage());
            attachTextAttachment(type + " Cleanup Error (" + identifier + ")", e.toString());
        }
    }

    private static void attachCleanupResponse(String title, Response response) {
        try {
            Allure.addAttachment(
                    title + " (Status: " + response.statusCode() + ")",
                    "application/json",
                    new ByteArrayInputStream(response.asPrettyString().getBytes()),
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
                    new ByteArrayInputStream(message.getBytes(StandardCharsets.UTF_8)), ".json"
            );
        } catch (Exception ignored) {}
    }

    @FunctionalInterface
    private interface CleanupAction {
        Response execute();
    }
}
