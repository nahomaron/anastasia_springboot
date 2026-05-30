package com.anastasia.Anastasia_BackEnd.Api.tests.avatar;

import com.anastasia.Anastasia_BackEnd.Api.base.BaseApiTest;
import com.anastasia.Anastasia_BackEnd.Api.config.RequestSpecFactory;
import com.anastasia.Anastasia_BackEnd.Api.services.ImageAssetService;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.imageasset.FinalizeImageUploadRequest;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.imageasset.ImageUploadRequest;
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

    private final ImageAssetService avatarService = new ImageAssetService();

    @Test
    @Story("Missing filename should be rejected")
    void requestingPresignedUrlWithoutFileNameShouldFail() {
        Response response = avatarService.requestPresignedUrl(
                RequestSpecFactory.authenticatedSpec(),
                "USER",
                BaseApiTest.getCachedUserId(),
                ImageUploadRequest.builder()
                        .fileName("")
                        .contentType("image/png")
                        .fileSizeBytes(1024L)
                        .build()
        );
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
                FinalizeImageUploadRequest.builder()
                        .uploadId(java.util.UUID.randomUUID())
                        .imageSize("1KB")
                        .build());

        // 3. Assert the result.
        assertThat(response.statusCode()).isGreaterThanOrEqualTo(400);
    }
}
