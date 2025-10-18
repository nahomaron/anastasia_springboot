package com.anastasia.Anastasia_BackEnd.api.config;

import com.anastasia.Anastasia_BackEnd.api.base.BaseApiTest;
import com.anastasia.Anastasia_BackEnd.model.role.Role;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

import java.util.Map;

/**
 * Factory for creating reusable RestAssured RequestSpecifications.
 * All specs include JSON content type and logging filters for visibility.
 */
public final class RequestSpecFactory {

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
     * Returns a specification with the cached authenticated token from BaseApiTest.
     * Suitable for any secured endpoints.
     */
    public static RequestSpecification authenticatedSpec() {
        if (BaseApiTest.getCachedAccessToken() == null) {
            BaseApiTest.ensureAuthenticated();
        }
        return new RequestSpecBuilder()
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .addHeader("Authorization", "Bearer " + BaseApiTest.getCachedAccessToken())
                .log(LogDetail.METHOD)
                .log(LogDetail.URI)
                .log(LogDetail.BODY)
                .build();
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
