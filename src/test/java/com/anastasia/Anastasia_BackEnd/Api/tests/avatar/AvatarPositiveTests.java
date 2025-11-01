package com.anastasia.Anastasia_BackEnd.Api.tests.avatar;

import com.anastasia.Anastasia_BackEnd.Api.base.BaseApiTest;
import com.anastasia.Anastasia_BackEnd.Api.config.RequestSpecFactory;
import com.anastasia.Anastasia_BackEnd.Api.factories.AvatarDataFactory;
import com.anastasia.Anastasia_BackEnd.Api.services.AvatarService;
import com.anastasia.Anastasia_BackEnd.Api.utils.SchemaValidator;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.avatar.AvatarDTO;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Epic("Avatar Management")
@Feature("Avatar happy paths")
@Severity(SeverityLevel.CRITICAL)
class AvatarPositiveTests extends BaseApiTest {

    private final AvatarService avatarService = new AvatarService();

    @Test
    @Story("User uploads and retrieves avatar successfully")
    void shouldGeneratePresignedUrlAndPersistAvatar() {
        String email = BaseApiTest.getCachedEmail();
        UUID userId = BaseApiTest.getCachedUserId();


        Response urlResponse = avatarService.requestPresignedUrl(RequestSpecFactory.authenticatedSpec(), "profile.png");
        assertThat(urlResponse.statusCode()).isEqualTo(200);
        SchemaValidator.validate(urlResponse, "schemas/avatar-presigned-url-schema.json");

        AvatarDTO payload = AvatarDataFactory.newValidAvatar();


        Response saveResponse = avatarService.saveAvatar(RequestSpecFactory.authenticatedSpec(), "USER", userId, payload);
        assertThat(saveResponse.statusCode()).isEqualTo(200);
        SchemaValidator.validate(saveResponse, "schemas/avatar-schema.json");

        Response fetchResponse = avatarService.getAvatar(RequestSpecFactory.authenticatedSpec(), "USER", userId);
        assertThat(fetchResponse.statusCode()).isEqualTo(200);
        SchemaValidator.validate(fetchResponse, "schemas/avatar-schema.json");
    }
}
