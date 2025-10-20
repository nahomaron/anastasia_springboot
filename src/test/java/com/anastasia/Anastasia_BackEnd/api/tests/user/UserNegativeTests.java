package com.anastasia.Anastasia_BackEnd.api.tests.user;

import com.anastasia.Anastasia_BackEnd.api.base.BaseApiTest;
import com.anastasia.Anastasia_BackEnd.api.config.RequestSpecFactory;
import com.anastasia.Anastasia_BackEnd.api.factories.UserDataFactory;
import com.anastasia.Anastasia_BackEnd.api.services.UserService;
import com.anastasia.Anastasia_BackEnd.model.user.UserDTO;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Epic("User Management")
@Feature("Negative coverage")
@Severity(SeverityLevel.NORMAL)
class UserNegativeTests extends BaseApiTest {

    private final UserService userService = new UserService();

    @Test
    @Story("Invalid email should trigger validation error")
    void updatingWithInvalidEmailShouldFail() {
        UserDTO payload = UserDataFactory.updatePayload();
        payload.setEmail("invalid-email");

        Response response = userService.updateDetails(RequestSpecFactory.authenticatedSpec(), payload);
        assertThat(response.statusCode()).isGreaterThanOrEqualTo(400);
    }

    @Test
    @Story("Assigning roles to unknown user returns error")
    void assigningRolesToUnknownUserShouldFail() {
        Response response = userService.assignRoles(getSpecForRole("OWNER"),
                UUID.randomUUID(),
                UserDataFactory.assignRolesRequest(Collections.singleton(999L)));

        assertThat(response.statusCode()).isGreaterThanOrEqualTo(400);
    }
}
