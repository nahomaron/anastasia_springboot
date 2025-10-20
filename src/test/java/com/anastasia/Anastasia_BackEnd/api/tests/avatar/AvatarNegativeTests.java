package com.anastasia.Anastasia_BackEnd.api.tests.avatar;

import com.anastasia.Anastasia_BackEnd.api.base.BaseApiTest;
import com.anastasia.Anastasia_BackEnd.api.config.RequestSpecFactory;
import com.anastasia.Anastasia_BackEnd.api.factories.AvatarDataFactory;
import com.anastasia.Anastasia_BackEnd.api.services.AvatarService;
import com.anastasia.Anastasia_BackEnd.api.utils.UserLookupHelper;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.util.UUID;

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
        var userId = UserLookupHelper.findUserIdByEmail(BaseApiTest.getCachedEmail());
        Assumptions.assumeTrue(userId.isPresent(), "User id available");

        Response response = avatarService.saveAvatar(
                RequestSpecFactory.authenticatedSpec(),
                "UNKNOWN",
                userId.orElse(UUID.randomUUID()),
                AvatarDataFactory.newValidAvatar());
        assertThat(response.statusCode()).isGreaterThanOrEqualTo(400);
    }
}
