package com.anastasia.Anastasia_BackEnd.api.config;

import com.anastasia.Anastasia_BackEnd.api.base.BaseApiTest;
import io.qameta.allure.Allure;
import io.restassured.builder.ResponseBuilder;
import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Objects;

public class ApiInterceptor extends BaseApiTest implements Filter {

    private static final Logger log = LoggerFactory.getLogger(ApiInterceptor.class);
    private static final List<String> PUBLIC_AUTH_ENDPOINTS = List.of(
            ConfigManager.get("auth.login.endpoint"),
            ConfigManager.get("auth.signup.endpoint"),
            ConfigManager.get("auth.activate.endpoint"),
            ConfigManager.get("test.activation.endpoint"),
            "/auth/login",
            "/auth/sign-up",
            "/auth/activate-account",
            "/auth/test/activation-token",
            "/auth/refresh-token",
            "/auth/logout"
    );

    @Override
    public Response filter(FilterableRequestSpecification requestSpec,
                           FilterableResponseSpecification responseSpec,
                           FilterContext ctx) {

        // Automatically add Authorization header if missing
        boolean isPublicAuthRequest = isPublicAuthEndpoint(requestSpec);
        if (!isPublicAuthRequest && cachedAuth != null && requestSpec.getHeaders().getValue("Authorization") == null) {
            requestSpec.header("Authorization", bearerToken());
        }

        log.info("➡️  [{}]  {}", requestSpec.getMethod(), requestSpec.getURI());
        if (requestSpec.getBody() != null)
            log.info("Request Body: {}", (Object) requestSpec.getBody());

        Response response = null; // 1. Initialize safely

        try {
            response = ctx.next(requestSpec, responseSpec); // attempt to call API

            // 2. Log safely
            if (response != null) {
                log.info("⬅️  Status: {}", response.getStatusCode());
                log.info("Response Body: {}", response.asPrettyString());

                // 3. Handle token expiration only if we have a real response
                boolean hasAuthorizationHeader = requestSpec.getHeaders().hasHeaderWithName("Authorization");
                if (!isPublicAuthRequest && hasAuthorizationHeader && response.getStatusCode() == 401) {
                    log.warn("Token expired — refreshing...");
                    initializeAuthenticationCache();
                    requestSpec.header("Authorization", bearerToken());
                    response = ctx.next(requestSpec, responseSpec);
                    if (response == null) {
                        log.warn("Token refresh retry returned null for: {}", requestSpec.getURI());
                        response = buildFailureResponse(503,
                                "Received null response after token refresh attempt for "
                                        + requestSpec.getMethod() + " " + requestSpec.getURI());
                    }
                }

            } else {
                log.warn("Response was null for request: {}", requestSpec.getURI());
                response = buildFailureResponse(503,
                        "Received null response for " + requestSpec.getMethod() + " " + requestSpec.getURI());
            }

        } catch (Exception e) {
            //4. Catch and log IO/network exceptions clearly
            log.error("Network or IO error during request to {}: {}", requestSpec.getURI(), e.getMessage(), e);
            int code = (e.getMessage() != null && e.getMessage().contains("Connection refused")) ? 503 : 500;

            response = buildFailureResponse(code,
                    "Request failed: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
        }

        Allure.addAttachment(
                "Response - " + response.getStatusCode(),
                "application/json",
                new ByteArrayInputStream(response.asPrettyString().getBytes()),
                ".json"
        );

        if (requestSpec.getBody() != null) {
            Allure.addAttachment(
                    "Request - " + requestSpec.getMethod() + " " + requestSpec.getURI(),
                    "application/json",
                    new ByteArrayInputStream(requestSpec.getBody().toString().getBytes()),
                    ".json"
            );
        }



        return response;
    }

    private Response buildFailureResponse(int statusCode, String message) {
        ResponseBuilder builder = new ResponseBuilder();
        builder.setStatusCode(statusCode);
        builder.setContentType(ContentType.JSON);
        builder.setBody("{\"message\":\"" + message.replace("\"", "\\\"") + "\"}");
        return builder.build();
    }

    private boolean isPublicAuthEndpoint(FilterableRequestSpecification requestSpec) {
        String uri = requestSpec.getURI();
        return PUBLIC_AUTH_ENDPOINTS.stream()
                .filter(Objects::nonNull)
                .anyMatch(uri::contains);
    }
}
