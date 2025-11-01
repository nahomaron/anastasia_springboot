package com.anastasia.Anastasia_BackEnd.Api.utils;

import io.qameta.allure.Allure;
import io.restassured.http.Header;
import io.restassured.module.jsv.JsonSchemaValidator;
import io.restassured.response.Response;
import org.junit.jupiter.api.Assertions;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Utility class to validate API responses against predefined JSON Schemas.
 */
public final class SchemaValidator {

    private static final String BASE_SCHEMA_PATH = "schemas/";

    // Explicit endpoint→schema mapping
    private static final Map<String, String> SCHEMA_REGISTRY = Map.ofEntries(
            Map.entry("/auth/login", "auth-login-schema.json"),
            Map.entry("/auth/sign-up", "auth-signup-schema.json"),
            Map.entry("/auth/activate-account", "auth-activate-schema.json"),
            Map.entry("/auth/refresh-token", "auth-refresh-schema.json"),
            Map.entry("/auth/logout", "auth-logout-schema.json")
    );

    private static final Pattern API_VERSION_PREFIX = Pattern.compile("^/?api/v\\d+/");

    private SchemaValidator() {}

    /**
     * Validates the response against the correct schema, inferred or mapped.
     */
    public static void validate(Response response) {
        String requestUri = extractRequestUri(response);
        String schemaPath = resolveSchemaPath(requestUri);
        validate(response, schemaPath);
    }

    public static void validate(Response response, String schemaPath) {
        try (InputStream schemaStream = SchemaValidator.class
                .getClassLoader()
                .getResourceAsStream(schemaPath)) {

            if (schemaStream == null) {
                throw new IllegalArgumentException("Schema not found: " + schemaPath);
            }

            // Attach schema to Allure report for traceability
            Allure.addAttachment(
                    "Schema: " + schemaPath,
                    "application/json",
                    schemaStream,
                    ".json"
            );

            // Reload schema stream for validation (Allure consumes it once)
            InputStream validationStream = SchemaValidator.class
                    .getClassLoader()
                    .getResourceAsStream(schemaPath);

            if (validationStream == null) {
                throw new IllegalArgumentException("Schema not found: " + schemaPath);
            }

            response.then()
                    .assertThat()
                    .body(JsonSchemaValidator.matchesJsonSchema(validationStream));

        } catch (Exception e) {
            String body = response != null ? response.asPrettyString() : "(no body)";
            Assertions.fail("Schema validation failed for " + schemaPath + ":\n" + e.getMessage() + "\nResponse:\n" + body);
        } finally {
            RequestTracker.clear();
        }
    }

    /**
     * Determines which schema file to use, first by registry, then fallback inference.
     */
    private static String resolveSchemaPath(String uri) {
        String normalizedPath = normalizePath(uri);
        if (normalizedPath.isEmpty()) {
            return BASE_SCHEMA_PATH + "unknown-schema.json";
        }

        String registeredSchema = findRegisteredSchema(normalizedPath);
        if (registeredSchema != null) {
            return BASE_SCHEMA_PATH + registeredSchema;
        }

        String slug = buildSchemaSlug(normalizedPath);
        if (slug.isEmpty()) {
            return BASE_SCHEMA_PATH + "unknown-schema.json";
        }

        return BASE_SCHEMA_PATH + slug + "-schema.json";
    }

    private static String findRegisteredSchema(String normalizedPath) {
        return SCHEMA_REGISTRY.entrySet().stream()
                .filter(entry -> matchesEndpoint(normalizedPath, normalizePath(entry.getKey())))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    private static boolean matchesEndpoint(String inspected, String candidate) {
        if (candidate.isEmpty()) {
            return false;
        }
        return inspected.equals(candidate) || inspected.endsWith(candidate);
    }

    /**
     * Extracts request URI safely from the RestAssured Response object.
     */
    private static String extractRequestUri(Response response) {
        try {
            String uriHeader = response.getHeader("X-Request-URI");
            if (uriHeader != null && !uriHeader.isBlank()) {
                return uriHeader;
            }

            String trackedUri = RequestTracker.getLastRequestUri();
            if (trackedUri != null && !trackedUri.isBlank()) {
                return trackedUri;
            }

            // fallback — look inside headers for URI-like values
            return response.getHeaders().asList().stream()
                    .map(Header::getValue)
                    .filter(v -> v.contains("/api/") || v.contains("/auth/"))
                    .findFirst()
                    .orElse("");
        } catch (Exception e) {
            return "";
        }
    }

    private static String normalizePath(String rawUri) {
        if (rawUri == null || rawUri.isBlank()) {
            return "";
        }
        String trimmed = rawUri.trim();
        try {
            URI uri = deriveUri(trimmed);
            String path = uri.getPath();
            if (path == null || path.isBlank()) {
                return "";
            }
            return sanitizePath(path);
        } catch (URISyntaxException e) {
            return sanitizePath(trimmed);
        }
    }

    private static URI deriveUri(String value) throws URISyntaxException {
        if (value.startsWith("http://") || value.startsWith("https://")) {
            return new URI(value);
        }
        if (value.startsWith("//")) {
            return new URI("http:" + value);
        }
        if (value.startsWith("/")) {
            return new URI(null, null, value, null);
        }
        return new URI(null, null, "/" + value, null);
    }

    private static String sanitizePath(String path) {
        String cleaned = path.replaceAll("/{2,}", "/");
        if (!cleaned.startsWith("/")) {
            cleaned = "/" + cleaned;
        }
        if (cleaned.endsWith("/") && cleaned.length() > 1) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }
        return cleaned;
    }

    private static String buildSchemaSlug(String normalizedPath) {
        String withoutApiPrefix = API_VERSION_PREFIX.matcher(normalizedPath).replaceFirst("");
        String withoutLeadingSlash = withoutApiPrefix.replaceAll("^/+", "");
        return withoutLeadingSlash
                .replaceAll("/", "-")
                .replaceAll("[^a-zA-Z0-9-]", "")
                .toLowerCase();
    }
}
