package com.anastasia.Anastasia_BackEnd.Api.config;

import com.anastasia.Anastasia_BackEnd.Api.base.BaseApiTest;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

import java.util.Map;
import java.util.UUID;

/**
 * Factory for creating reusable RestAssured RequestSpecifications.
 * All specs include JSON content type and logging filters for visibility.
 */
public final class RequestSpecFactory {
    private static final String TEST_HELPER_SECRET_HEADER = "X-Test-Helper-Secret";

    private RequestSpecFactory() {
    }

    /**
     * Returns a basic unauthenticated specification.
     * Suitable for public or login endpoints.
     */
    public static RequestSpecification anonymousSpec() {
        return new RequestSpecBuilder()
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .log(LogDetail.METHOD)
                .log(LogDetail.URI)
                .log(LogDetail.BODY)
                .build();
    }

    /**
     * Returns an unauthenticated specification allowed to call profile-gated test helper endpoints.
     */
    public static RequestSpecification testHelperSpec() {
        RequestSpecBuilder builder = new RequestSpecBuilder()
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .log(LogDetail.METHOD)
                .log(LogDetail.URI)
                .log(LogDetail.BODY);

        String secret = ConfigManager.get("app.security.test-helper-secret");
        if (secret != null && !secret.isBlank()) {
            builder.addHeader(TEST_HELPER_SECRET_HEADER, secret.trim());
        }

        return builder.build();
    }

    /**
     * Returns a specification with the cached authenticated token from BaseApiTest.
     * Suitable for any secured endpoints.
     */
    public static RequestSpecification authenticatedSpec() {
        BaseApiTest.ensureAuthenticated();
        RequestSpecBuilder builder = new RequestSpecBuilder()
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .addHeader("Authorization", "Bearer " + BaseApiTest.getCachedAccessToken())
                .log(LogDetail.METHOD)
                .log(LogDetail.URI)
                .log(LogDetail.BODY);

        UUID tenantId = BaseApiTest.getCachedTenantId();
        if (tenantId != null) {
            builder.addHeader("X-Tenant-ID", tenantId.toString());
        }

        return builder.build();
    }

    /**
     * Returns a spec configured for a specific role (e.g., ADMIN, OWNER, MEMBER).
     */
    public static RequestSpecification specForRole(String role) {
        return BaseApiTest.getSpecForRole(role);
    }

    /**
     * Builds a custom RequestSpecification with additional headers.
     */
    public static RequestSpecification specWithHeaders(Map<String, String> headers) {
        RequestSpecBuilder builder = new RequestSpecBuilder()
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .log(LogDetail.METHOD)
                .log(LogDetail.URI)
                .log(LogDetail.BODY);

        if (headers != null && !headers.isEmpty()) {
            headers.forEach(builder::addHeader);
        }

        return builder.build();
    }
}
