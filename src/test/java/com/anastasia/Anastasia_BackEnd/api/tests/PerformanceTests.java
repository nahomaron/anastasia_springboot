package com.anastasia.Anastasia_BackEnd.api.tests;

import com.anastasia.Anastasia_BackEnd.api.base.BaseApiTest;
import com.anastasia.Anastasia_BackEnd.api.services.AuthService;
import com.anastasia.Anastasia_BackEnd.model.auth.AuthenticationRequest;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Epic("Performance & Scalability")
@Feature("API Latency Baseline")
@Owner("Nahom Aron")
@Severity(SeverityLevel.NORMAL)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PerformanceTests extends BaseApiTest {

    private final AuthService authService = new AuthService();

    private static final int RUNS = 20;

    @Test
    @Order(1)
    @DisplayName("Login Endpoint Performance")
    @Description("Measures latency for /auth/login")
    void measureLoginPerformance() {
        AuthenticationRequest request = new AuthenticationRequest(cachedEmail, cachedPassword);
        measureEndpoint("POST /auth/login", () -> authService.login(request), 200);
    }

//    @Test
//    @Order(2)
//    @DisplayName("Get Members Performance")
//    @Description("Measures latency for /members endpoint (GET)")
//    void measureMembersPerformance() {
//        measureEndpoint("GET /members", () ->
//                RestAssured.given()
//                        .spec(getAuthenticatedSpec())
//                        .get("/members")
//                        .andReturn(), 200);
//    }

//    @Test
//    @Order(3)
//    @DisplayName("Get Tenants Performance")
//    @Description("Measures latency for /tenants endpoint (GET)")
//    void measureTenantsPerformance() {
//        measureEndpoint("GET /tenants", () ->
//                RestAssured.given()
//                        .spec(getAuthenticatedSpec())
//                        .get("/tenants")
//                        .andReturn(), 200);
//    }


    private void measureEndpoint(String label,
                                 java.util.function.Supplier<Response> apiCall,
                                 int expectedStatus) {

        List<Long> durations = new ArrayList<>();
        int runs = 20;

        for (int i = 0; i < runs; i++) {
            long start = System.currentTimeMillis();
            Response res = apiCall.get();
            long end = System.currentTimeMillis();
            durations.add(end - start);

            assertThat(res.statusCode())
                    .as("Expected %s to return %d", label, expectedStatus)
                    .isEqualTo(expectedStatus);
        }

        double avg = durations.stream().mapToLong(Long::longValue).average().orElse(0);
        long p95 = durations.stream().sorted().skip((long) (runs * 0.95) - 1).findFirst().orElse(0L);

        // Add thresholds (adjust to your expectations)
        long MAX_AVG_MS = 500;   // fail if average > 500ms
        long MAX_P95_MS = 800;   // fail if P95 > 800ms

        assertThat(avg)
                .as("%s average response time (%.2f ms) exceeded threshold (%d ms)", label, avg, MAX_AVG_MS)
                .isLessThan(MAX_AVG_MS);

        assertThat(p95)
                .as("%s P95 response time (%d ms) exceeded threshold (%d ms)", label, p95, MAX_P95_MS)
                .isLessThan(MAX_P95_MS);

        // Write summary attachment for Allure
        String summary = String.format(
                "%s%nRuns: %d%nAverage: %.2f ms (limit %d)%nP95: %d ms (limit %d)",
                label, runs, avg, MAX_AVG_MS, p95, MAX_P95_MS
        );

        Allure.addAttachment("Performance Summary - " + label, "text/plain", summary);
        System.out.println(summary);
    }

}
