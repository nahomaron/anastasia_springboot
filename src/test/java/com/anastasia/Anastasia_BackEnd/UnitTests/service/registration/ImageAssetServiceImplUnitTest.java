package com.anastasia.Anastasia_BackEnd.UnitTests.service.registration;

import com.anastasia.Anastasia_BackEnd.UnitTests.support.LenientMockitoTest;
import com.anastasia.Anastasia_BackEnd.common.aws.PresignedUrlResponse;
import com.anastasia.Anastasia_BackEnd.common.aws.S3Service;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ImageAssetRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.ImageAssetServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@LenientMockitoTest
class ImageAssetServiceImplUnitTest {

    @Mock private S3Service s3Service;
    @Mock private ImageAssetRepository imageAssetRepository;
    @Mock private Environment environment;

    @InjectMocks private ImageAssetServiceImpl imageAssetService;

    @Test
    void requestPresignedUrl_shouldFallbackToMockUrlForDevLocalhostS3() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});
        when(s3Service.generatePresignedUploadUrl("avatar.jpg"))
                .thenThrow(new RuntimeException("Localstack unavailable"));
        ReflectionTestUtils.setField(imageAssetService, "s3Endpoint", "http://localhost:4566");

        PresignedUrlResponse response = imageAssetService.requestPresignedUrl("avatar.jpg");

        assertThat(response.getUploadUrl()).isEqualTo("http://localhost/mock-presigned-url");
        assertThat(response.getObjectKey()).startsWith("test-imageAssets/");
    }

    @Test
    void requestPresignedUrl_shouldPropagateFailureOutsideDev() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"staging"});
        when(s3Service.generatePresignedUploadUrl("avatar.jpg"))
                .thenThrow(new RuntimeException("S3 unavailable"));
        ReflectionTestUtils.setField(imageAssetService, "s3Endpoint", "https://s3.amazonaws.com");

        assertThatThrownBy(() -> imageAssetService.requestPresignedUrl("avatar.jpg"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("S3 unavailable");
    }
}
