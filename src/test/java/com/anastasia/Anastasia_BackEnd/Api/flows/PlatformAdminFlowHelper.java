package com.anastasia.Anastasia_BackEnd.Api.flows;

import com.anastasia.Anastasia_BackEnd.Api.config.ConfigManager;
import com.anastasia.Anastasia_BackEnd.Api.config.RequestSpecFactory;
import com.anastasia.Anastasia_BackEnd.Api.services.AuthService;
import com.anastasia.Anastasia_BackEnd.core.auth.dto.AuthenticationRequest;
import com.anastasia.Anastasia_BackEnd.core.auth.dto.AuthenticationResponse;
import com.anastasia.Anastasia_BackEnd.core.auth.dto.PlatformAdminRegistrationRequest;
import io.restassured.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import static io.restassured.RestAssured.given;

public final class PlatformAdminFlowHelper {
    private static final Logger log = LoggerFactory.getLogger(PlatformAdminFlowHelper.class);
    private static final AuthService authService = new AuthService();
    private static final String DEFAULT_SECRET = "dev-secret";

    private PlatformAdminFlowHelper() {}

    public static AuthenticationResponse registerAndLogin(String email, String password) {
        String secret = ConfigManager.get("platform.admin.secret");
        if (secret == null || secret.isBlank()) {
            secret = DEFAULT_SECRET;
        }

        PlatformAdminRegistrationRequest dto = new PlatformAdminRegistrationRequest();
        dto.setFullName("Platform Admin " + email);
        dto.setEmail(email);
        dto.setPassword(password);

        Response register = given()
                .spec(RequestSpecFactory.anonymousSpec())
                .header("X-Platform-Admin-Secret", secret)
                .body(dto)
                .post("/auth/platform-admin/register");
        if (register.statusCode() != 201) {
            throw new RuntimeException("Platform admin registration failed: " + register.statusCode() + " " + register.asString());
        }

        AuthenticationResponse login = authService.loginAndExtractToken(new AuthenticationRequest(email, password));
        if (login == null || login.getAccessToken() == null) {
            throw new RuntimeException("Failed to login platform admin" + register.asString());
        }
        return login;
    }
}
