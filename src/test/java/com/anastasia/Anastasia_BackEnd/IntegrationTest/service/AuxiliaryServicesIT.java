package com.anastasia.Anastasia_BackEnd.IntegrationTest.service;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.imageasset.ImageAssetDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.imageasset.FinalizeImageUploadRequest;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.imageasset.ImageUploadRequest;
import com.anastasia.Anastasia_BackEnd.core.auth.role.Role;
import com.anastasia.Anastasia_BackEnd.core.auth.role.RoleType;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.ImageAssetService;
import com.anastasia.Anastasia_BackEnd.TestSupport.ServiceIntegrationTestBase;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Epic("Integration Tests")
@Feature("Service Layer - Auxiliary Services")
class AuxiliaryServicesIT extends ServiceIntegrationTestBase {

    @Autowired private ImageAssetService imageAssetService;

    private UserEntity user;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        Role ownerRole = fetchRole(RoleType.OWNER);
        user = persistUser("aux+" + UUID.randomUUID() + "@integration.com", ownerRole);
        authenticate(user);
    }

    @Test
    void imageAssetService_returnsPresignedUrlAndRetrievesAvatar() {
        var presigned = imageAssetService.requestPresignedUrl(
                "USER",
                user.getUuid().toString(),
                ImageUploadRequest.builder()
                        .fileName("profile.png")
                        .contentType("image/png")
                        .fileSizeBytes(1024L)
                        .build()
        );
        assertThat(presigned.getUploadId()).isNotNull();
        assertThat(presigned.getObjectKey()).contains("/image-assets/user/");
        assertThat(presigned.getUploadUrl()).contains("mock-presigned-url");
        assertThat(presigned.getObjectUrl()).contains("mock-bucket");

        imageAssetService.saveImageAsset(
                "USER",
                user.getUuid().toString(),
                FinalizeImageUploadRequest.builder()
                        .uploadId(presigned.getUploadId())
                        .imageSize("15KB")
                        .build()
        );

        ImageAssetDTO fetched = imageAssetService.getImageAsset("USER", user.getUuid().toString());
        assertThat(fetched.getImageUrl()).isEqualTo(presigned.getObjectUrl());
        assertThat(fetched.getImageSize()).isEqualTo("15KB");
    }
}
