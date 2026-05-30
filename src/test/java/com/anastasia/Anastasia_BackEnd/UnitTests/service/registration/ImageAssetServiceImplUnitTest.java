package com.anastasia.Anastasia_BackEnd.UnitTests.service.registration;

import com.anastasia.Anastasia_BackEnd.UnitTests.support.LenientMockitoTest;
import com.anastasia.Anastasia_BackEnd.common.aws.PresignedUrlResponse;
import com.anastasia.Anastasia_BackEnd.common.aws.S3Service;
import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.core.auth.principal.UserPrincipal;
import com.anastasia.Anastasia_BackEnd.core.auth.role.Role;
import com.anastasia.Anastasia_BackEnd.modules.events.repository.EventRepository;
import com.anastasia.Anastasia_BackEnd.modules.groups.GroupRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.imageasset.ImageUploadRequest;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.imageasset.ImageAssetUploadIntentEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ImageAssetRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ImageAssetUploadIntentRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.ImageAssetServiceImpl;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@LenientMockitoTest
class ImageAssetServiceImplUnitTest {

    @Mock private S3Service s3Service;
    @Mock private ImageAssetRepository imageAssetRepository;
    @Mock private ImageAssetUploadIntentRepository uploadIntentRepository;
    @Mock private EventRepository eventRepository;
    @Mock private GroupRepository groupRepository;
    @Mock private Environment environment;

    @InjectMocks private ImageAssetServiceImpl imageAssetService;

    @Test
    void requestPresignedUrl_shouldFallbackToMockUrlForDevLocalhostS3() {
        UUID tenantId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        authenticate(userId);
        TenantContext.setTenantId(tenantId);
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});
        when(uploadIntentRepository.save(any(ImageAssetUploadIntentEntity.class))).thenAnswer(invocation -> {
            ImageAssetUploadIntentEntity intent = invocation.getArgument(0);
            intent.setId(UUID.randomUUID());
            intent.setCreatedAt(Instant.now());
            return intent;
        });
        when(s3Service.generatePresignedUploadUrl(anyString(), eq("image/png")))
                .thenThrow(new RuntimeException("Localstack unavailable"));
        when(s3Service.buildObjectUrl(anyString())).thenAnswer(invocation -> "http://localhost/mock-bucket/" + invocation.getArgument(0));
        ReflectionTestUtils.setField(imageAssetService, "s3Endpoint", "http://localhost:4566");

        PresignedUrlResponse response = imageAssetService.requestPresignedUrl(
                "USER",
                userId.toString(),
                ImageUploadRequest.builder()
                        .fileName("avatar.jpg")
                        .contentType("image/png")
                        .fileSizeBytes(1024L)
                        .build()
        );

        assertThat(response.getUploadUrl()).isEqualTo("http://localhost/mock-presigned-url");
        assertThat(response.getObjectUrl()).contains("mock-bucket");
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @Test
    void requestPresignedUrl_shouldPropagateFailureOutsideDev() {
        UUID tenantId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        authenticate(userId);
        TenantContext.setTenantId(tenantId);
        when(environment.getActiveProfiles()).thenReturn(new String[]{"staging"});
        when(uploadIntentRepository.save(any(ImageAssetUploadIntentEntity.class))).thenAnswer(invocation -> {
            ImageAssetUploadIntentEntity intent = invocation.getArgument(0);
            intent.setId(UUID.randomUUID());
            intent.setCreatedAt(Instant.now());
            return intent;
        });
        when(s3Service.generatePresignedUploadUrl(anyString(), eq("image/png")))
                .thenThrow(new RuntimeException("S3 unavailable"));
        ReflectionTestUtils.setField(imageAssetService, "s3Endpoint", "https://s3.amazonaws.com");

        assertThatThrownBy(() -> imageAssetService.requestPresignedUrl(
                "USER",
                userId.toString(),
                ImageUploadRequest.builder()
                        .fileName("avatar.jpg")
                        .contentType("image/png")
                        .fileSizeBytes(1024L)
                        .build()
        ))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("S3 unavailable");
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    private void authenticate(UUID userId) {
        UserEntity user = UserEntity.builder()
                .uuid(userId)
                .email("test@example.com")
                .roles(Set.of(Role.builder().roleName("USER").build()))
                .build();
        UserPrincipal principal = new UserPrincipal(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }
}
