package com.anastasia.Anastasia_BackEnd.api.config;

import com.anastasia.Anastasia_BackEnd.api.base.BaseApiTest;
import io.qameta.allure.Allure;
import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;

public class ApiInterceptor extends BaseApiTest implements Filter {

    private static final Logger log = LoggerFactory.getLogger(ApiInterceptor.class);

    @Override
    public Response filter(FilterableRequestSpecification requestSpec,
                           FilterableResponseSpecification responseSpec,
                           FilterContext ctx) {

        // Automatically add Authorization header if missing
        if (cachedAuth != null && requestSpec.getHeaders().getValue("Authorization") == null) {
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
                if (response.getStatusCode() == 401) {
                    log.warn("Token expired — refreshing...");
                    initializeAuthenticationCache();
                    requestSpec.header("Authorization", bearerToken());
                    response = ctx.next(requestSpec, responseSpec);
                }

            } else {
                log.warn("Response was null for request: {}", requestSpec.getURI());
            }

        } catch (Exception e) {
            //4. Catch and log IO/network exceptions clearly
            log.error("Network or IO error during request to {}: {}", requestSpec.getURI(), e.getMessage());
        }

//        String requestAttachment = requestSpec.getBody() != null
//                ? requestSpec.getBody().toString()
//                : "";
//        Allure.addAttachment(
//                "Request - " + requestSpec.getMethod() + " " + requestSpec.getURI(),
//                "application/json",
//                requestAttachment
//        );
//
//        if (response != null) {
//            Allure.addAttachment(
//                    "Response - " + response.getStatusCode(),
//                    "application/json",
//                    response.asPrettyString()
//            );
//        }
        if (response != null) {
            Allure.addAttachment(
                    "Response - " + response.getStatusCode(),
                    "application/json",
                    new ByteArrayInputStream(response.asPrettyString().getBytes()),
                    ".json"
            );
        }

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
}
