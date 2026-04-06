package com.anastasia.Anastasia_BackEnd.Api.config;

import com.anastasia.Anastasia_BackEnd.Api.base.BaseApiTest;
import com.anastasia.Anastasia_BackEnd.Api.utils.RequestTracker;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.qameta.allure.Allure;
import io.restassured.RestAssured;
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
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

public class ApiInterceptor extends BaseApiTest implements Filter {

    private static final Logger log = LoggerFactory.getLogger(ApiInterceptor.class);
    private static final ObjectWriter PRETTY_WRITER = new ObjectMapper().writerWithDefaultPrettyPrinter();


    private static final List<String> PUBLIC_AUTH_ENDPOINTS = buildPublicAuthEndpoints();

    @Override
    public Response filter(FilterableRequestSpecification requestSpec,
                           FilterableResponseSpecification responseSpec,
                           FilterContext ctx) {

        String resolvedUri = requestSpec.getURI();
        RequestTracker.record(resolvedUri);
        requestSpec.header("X-Request-URI", resolvedUri);

        boolean isPublicAuthRequest = isPublicAuthEndpoint(requestSpec);

        log.info("➡️  [{}]  {}", requestSpec.getMethod(), requestSpec.getURI());
        if (requestSpec.getBody() != null)
            log.info("Request Body: {}", (Object) requestSpec.getBody());
        if (!isPublicAuthRequest && requestSpec.getHeaders().hasHeaderWithName("Authorization")) {
            log.debug("Request already carries Authorization header for {}", requestSpec.getURI());
        }

        Response response = null;

        try {
            response = ctx.next(requestSpec, responseSpec);
            if (response != null) {
                log.info("⬅️  Status: {}", response.getStatusCode());
//                log.info("Response Body: {}", response.asPrettyString());

            } else {
                log.warn("Response was null for request: {}", requestSpec.getURI());
                response = buildFailureResponse(503,
                        "Received null response for " + requestSpec.getMethod() + " " + requestSpec.getURI());
            }

        } catch (Exception e) {
            log.error("Network or IO error during request to {}: {}", requestSpec.getURI(), e.getMessage(), e);
            int code = (e.getMessage() != null && e.getMessage().contains("Connection refused")) ? 503 : 500;

            response = buildFailureResponse(code,
                    "Request failed: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
        }

        attachRequestAndResponse(requestSpec, response);

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
        String path = extractPath(requestSpec);
        if (!hasLength(path)) {
            return false;
        }
        return PUBLIC_AUTH_ENDPOINTS.stream()
                .filter(Objects::nonNull)
                .anyMatch(path::startsWith);
    }

    private String extractPath(FilterableRequestSpecification requestSpec) {
        String raw = requestSpec.getURI();
        try {
            URI uri = new URI(raw);
            String path = uri.getPath();
            if (hasLength(path)) {
                return normalizePath(path);
            }
        } catch (URISyntaxException e) {
            log.debug("Unable to parse URI for public endpoint check: {}", raw);
        }
        return normalizePath(stripBasePath(requestSpec));
    }

    private static boolean hasLength(String value) {
        return value != null && !value.isBlank();
    }

    private String normalizePath(String path) {
        if (!hasLength(path)) {
            return path;
        }
        return path.replaceAll("/{2,}", "/");
    }

    private static List<String> buildPublicAuthEndpoints() {
        return List.of(
                        normalizePublicEndpoint(ConfigManager.get("auth.login.endpoint")),
                        normalizePublicEndpoint(ConfigManager.get("auth.signup.endpoint")),
                        normalizePublicEndpoint(ConfigManager.get("auth.activate.endpoint")),
                        normalizePublicEndpoint(ConfigManager.get("test.activation.endpoint")),
                        normalizePublicEndpoint(ConfigManager.get("test.tenant.otp.endpoint")),
                        "/auth/login",
                        "/auth/sign-up",
                        "/auth/activate-account",
                        "/auth/test/activation-token",
                        "/tenant/test/otp",
                        "/auth/refresh-token",
                        "/auth/logout",
                        "/auth/platform-admin/register",
                        "/tenant/subscription",
                        "/tenant/verify-phone",
                        "/tenant/resend-phone-otp",
                        "/onboarding/email-verification/send-code",
                        "/onboarding/email-verification/verify-code",
                        "/onboarding/billing/sessions",
                        "/test-utils"
                )
                .stream()
                .filter(Objects::nonNull)
                .toList();
    }

    private static String normalizePublicEndpoint(String value) {
        if (!hasLength(value)) {
            return null;
        }
        String normalized = value.trim();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        normalized = normalized.replaceAll("/{2,}", "/");
        return normalized;
    }

    private static String stripBasePath(FilterableRequestSpecification requestSpec) {
        String baseUri = RestAssured.baseURI;
        String fullUri = requestSpec.getURI();
        if (!hasLength(baseUri)) {
            return fullUri;
        }
        if (fullUri.startsWith(baseUri)) {
            return fullUri.substring(baseUri.length());
        }
        return fullUri;
    }

    private void attachRequestAndResponse(FilterableRequestSpecification req, Response res) {
        try {
            if (Allure.getLifecycle().getCurrentTestCase().isPresent()) {
                if (req.getBody() != null) {
                    String body = formatJsonSafely(req.getBody().toString());
                    Allure.addAttachment(
                            "📤 Request → " + req.getMethod() + " " + req.getURI(),
                            "application/json",
                            new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)),
                            ".json"
                    );
                }

                if (res != null && res.getBody() != null) {
                    String body = formatJsonSafely(res.asPrettyString());
                    Allure.addAttachment(
                            "📥 Response ← " + res.getStatusCode() + " " + req.getURI(),
                            "application/json",
                            new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)),
                            ".json"
                    );
                }
            }
        } catch (Exception e) {
            log.warn("Failed to attach Allure request/response: {}", e.getMessage());
        }
    }

    private String formatJsonSafely(String text) {
        try {
            Object json = new ObjectMapper().readValue(text, Object.class);
            return PRETTY_WRITER.writeValueAsString(json);
        } catch (Exception e) {
            return text; // not JSON, return raw
        }
    }
}
