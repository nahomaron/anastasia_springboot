package com.anastasia.Anastasia_BackEnd.Api.tests.user;

import com.anastasia.Anastasia_BackEnd.Api.base.BaseApiTest;
import com.anastasia.Anastasia_BackEnd.Api.config.RequestSpecFactory;
import com.anastasia.Anastasia_BackEnd.Api.factories.AvatarDataFactory;
import com.anastasia.Anastasia_BackEnd.Api.factories.UserDataFactory;
import com.anastasia.Anastasia_BackEnd.Api.services.UserService;
import com.anastasia.Anastasia_BackEnd.Api.utils.SchemaValidator;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.imageasset.ImageAssetDTO;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserDTO;
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
        ImageAssetDTO payload = AvatarDataFactory.newValidAvatar();
        Response response = userService.updateAvatar(RequestSpecFactory.authenticatedSpec(), payload);
        assertThat(response.statusCode()).isEqualTo(200);
    }
}
