package com.anastasia.Anastasia_BackEnd.api.utils;

import io.qameta.allure.Allure;
import io.restassured.module.jsv.JsonSchemaValidator;
import io.restassured.response.Response;
import org.junit.jupiter.api.Assertions;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Utility class to validate API responses against predefined JSON Schemas.
 */
public final class SchemaValidator {

    private SchemaValidator() {}

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

            response.then()
                    .assertThat()
                    .body(JsonSchemaValidator.matchesJsonSchema(validationStream));

        } catch (Exception e) {
            String body = response != null ? response.asPrettyString() : "(no body)";
            Assertions.fail("Schema validation failed for " + schemaPath + ":\n" + e.getMessage() + "\nResponse:\n" + body);
        }
    }
}
