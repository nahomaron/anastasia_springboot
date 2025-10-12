package com.anastasia.Anastasia_BackEnd.api.base;

import com.anastasia.Anastasia_BackEnd.api.config.ConfigManager;
import com.anastasia.Anastasia_BackEnd.api.services.AuthService;
import com.anastasia.Anastasia_BackEnd.api.tests.AuthFlowHelper;
import com.anastasia.Anastasia_BackEnd.model.auth.AuthenticationRequest;
import com.anastasia.Anastasia_BackEnd.model.auth.AuthenticationResponse;
import com.anastasia.Anastasia_BackEnd.util.JwtUtil;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.parsing.Parser;
import io.restassured.specification.RequestSpecification;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeAll;

@RequiredArgsConstructor
public class BaseApiTest {

    private static final JwtUtil jwtUtil = new JwtUtil();
    private static AuthService authService;

    protected static AuthenticationResponse cachedAuth;
    protected static RequestSpecification authSpec;

    @BeforeAll
    public static void setUp(){
        RestAssured.baseURI = ConfigManager.get("base.url");
        RestAssured.defaultParser = Parser.JSON;  // Tell RestAssured to treat everything as JSON


        // 2️⃣ Sign up + activate + login once if not already cached
        if (cachedAuth == null) {
            String email = "api_user_" + System.currentTimeMillis() + "@mail.com";
            cachedAuth = AuthFlowHelper.signUpAndActivateAndLogin(email);
        }

        authSpec = new RequestSpecBuilder()
                .setContentType("application/json")
                .addHeader("Authorization", "Bearer " + cachedAuth.getAccessToken())
                .build();
    }


    /**
     * Utility to get the authenticated spec from any test
     */
    public static RequestSpecification getAuthenticatedSpec() {
        if (jwtUtil.isTokenExpired(cachedAuth.getAccessToken())) {
            String email = jwtUtil.extractUsername(cachedAuth.getAccessToken());

            AuthenticationRequest request = new AuthenticationRequest(email, "Password@123");
            cachedAuth = authService.loginAndExtractToken(request);
        }

        return authSpec;
    }
}
