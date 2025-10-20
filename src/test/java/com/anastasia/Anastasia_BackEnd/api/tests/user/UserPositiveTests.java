package com.anastasia.Anastasia_BackEnd.api.tests.user;

import com.anastasia.Anastasia_BackEnd.api.base.BaseApiTest;
import com.anastasia.Anastasia_BackEnd.api.config.RequestSpecFactory;
import com.anastasia.Anastasia_BackEnd.api.factories.AvatarDataFactory;
import com.anastasia.Anastasia_BackEnd.api.factories.UserDataFactory;
import com.anastasia.Anastasia_BackEnd.api.services.UserService;
import com.anastasia.Anastasia_BackEnd.api.utils.SchemaValidator;
import com.anastasia.Anastasia_BackEnd.model.avatar.AvatarDTO;
import com.anastasia.Anastasia_BackEnd.model.user.UserDTO;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Epic("User Management")
@Feature("Happy flows")
@Severity(SeverityLevel.CRITICAL)
class UserPositiveTests extends BaseApiTest {

    private final UserService userService = new UserService();

    @Test
    @Story("Owner dashboard is accessible")
    void ownerCanAccessDashboard() {
        Response response = userService.getDashboard(getSpecForRole("OWNER"));
        assertThat(response.statusCode()).isEqualTo(200);
    }

    @Test
    @Story("User updates profile details")
    void userCanUpdateProfileDetails() {
        UserDTO payload = UserDataFactory.updatePayload();
        RequestSpecification authSpec = RequestSpecFactory.authenticatedSpec();

        Response response = userService.updateDetails(authSpec, payload);
        assertThat(response.statusCode()).isIn(200, 202);
        SchemaValidator.validate(response, "schemas/user-dto-schema.json");
    }

    @Test
    @Story("User updates profile avatar")
    void userCanUpdateAvatar() {
        AvatarDTO payload = AvatarDataFactory.newValidAvatar();
        Response response = userService.updateAvatar(RequestSpecFactory.authenticatedSpec(), payload);
        assertThat(response.statusCode()).isEqualTo(200);
    }
}
