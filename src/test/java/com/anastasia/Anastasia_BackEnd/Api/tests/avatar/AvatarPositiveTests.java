package com.anastasia.Anastasia_BackEnd.Api.tests.avatar;

import com.anastasia.Anastasia_BackEnd.Api.base.BaseApiTest;
import com.anastasia.Anastasia_BackEnd.Api.services.ImageAssetService;
import com.anastasia.Anastasia_BackEnd.Api.utils.SchemaValidator;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.imageasset.FinalizeImageUploadRequest;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.imageasset.ImageUploadRequest;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Epic("Avatar Management")
@Feature("Avatar happy paths")
@Severity(SeverityLevel.CRITICAL)
class AvatarPositiveTests extends BaseApiTest {

    private final ImageAssetService avatarService = new ImageAssetService();

    @Test
    @Story("User uploads and retrieves avatar successfully")
    void shouldGeneratePresignedUrlAndPersistAvatar() {
        RequestSpecification ownerSpec = getSpecForRole("OWNER");
        UUID userId = BaseApiTest.getOwnerUserId();

        Response urlResponse = avatarService.requestPresignedUrl(ownerSpec, "USER", userId, ImageUploadRequest.builder()
                .fileName("profile.png")
                .contentType("image/png")
                .fileSizeBytes(1024L)
                .build());
        assertThat(urlResponse.statusCode()).isEqualTo(200);
        SchemaValidator.validate(urlResponse, "schemas/avatar-presigned-url-schema.json");

        String uploadId = urlResponse.jsonPath().getString("uploadId");
        Response saveResponse = avatarService.saveAvatar(ownerSpec, "USER", userId, FinalizeImageUploadRequest.builder()
                .uploadId(UUID.fromString(uploadId))
                .imageSize("256KB")
                .build());
        assertThat(saveResponse.statusCode()).isEqualTo(200);
        SchemaValidator.validate(saveResponse, "schemas/avatar-schema.json");

        Response fetchResponse = avatarService.getAvatar(ownerSpec, "USER", userId);
        assertThat(fetchResponse.statusCode()).isEqualTo(200);
        SchemaValidator.validate(fetchResponse, "schemas/avatar-schema.json");
    }
}
