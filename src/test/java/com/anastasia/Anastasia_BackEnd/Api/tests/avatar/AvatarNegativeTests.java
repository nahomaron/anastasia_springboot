package com.anastasia.Anastasia_BackEnd.Api.tests.avatar;

import com.anastasia.Anastasia_BackEnd.Api.base.BaseApiTest;
import com.anastasia.Anastasia_BackEnd.Api.config.RequestSpecFactory;
import com.anastasia.Anastasia_BackEnd.Api.factories.AvatarDataFactory;
import com.anastasia.Anastasia_BackEnd.Api.services.AvatarService;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Epic("Avatar Management")
@Feature("Avatar negative coverage")
@Severity(SeverityLevel.NORMAL)
class AvatarNegativeTests extends BaseApiTest {

    private final AvatarService avatarService = new AvatarService();

    @Test
    @Story("Missing filename should be rejected")
    void requestingPresignedUrlWithoutFileNameShouldFail() {
        Response response = avatarService.requestPresignedUrl(RequestSpecFactory.authenticatedSpec(), "");
        assertThat(response.statusCode()).isGreaterThanOrEqualTo(400);
    }

    @Test
    @Story("Invalid owner type is rejected")
    void savingAvatarWithUnknownOwnerTypeShouldFail() {
        var userId = BaseApiTest.getCachedUserId();

        Response response = avatarService.saveAvatar(
                RequestSpecFactory.authenticatedSpec(),
                "UNKNOWN",
                userId, // Use the extracted UUID directly
                AvatarDataFactory.newValidAvatar());

        // 3. Assert the result.
        assertThat(response.statusCode()).isGreaterThanOrEqualTo(400);
    }
}
