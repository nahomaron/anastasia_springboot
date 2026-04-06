package com.anastasia.Anastasia_BackEnd.modules.registration.service;

import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.imageasset.ImageAssetDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.imageasset.ImageAssetVisibility;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.imageasset.ImageAssetEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.imageasset.ImageStorageProvider;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.imageasset.ImageAssetType;
import com.anastasia.Anastasia_BackEnd.common.aws.PresignedUrlResponse;
import com.anastasia.Anastasia_BackEnd.common.aws.S3Service;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ImageAssetRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageAssetServiceImpl implements ImageAssetService{

    private final S3Service s3Service;
    private final ImageAssetRepository imageAssetRepository;

    @Value("${aws.s3.endpoint:http://localhost:4566}")
    private String s3Endpoint;

    @Override
    public PresignedUrlResponse requestPresignedUrl(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fileName must not be blank");
        }
        try {
            return s3Service.generatePresignedUploadUrl(fileName);
        } catch (RuntimeException ex) {
            if (isFallbackEnabled()) {
                log.warn("Unable to generate presigned URL via S3 ({}). Falling back to mock response.", ex.getMessage());
                return mockPresignedUrl(fileName);
            }
            throw ex;
        }
    }

    @CachePut(value = "imageAssets", key = "#root.target.buildImageAssetCacheKey(#ownerType, #ownerId)")
    @Override
    public ImageAssetDTO saveImageAsset(String ownerType, UUID ownerId, ImageAssetDTO imageAssetDTO) {
        ImageAssetType imageAssetType = resolveImageAssetType(ownerType);
        UUID tenantId = requireTenantId();

        ImageAssetEntity imageAsset = imageAssetRepository
                .findByTenantIdAndOwnerIdAndImageAssetTypeAndDeletedAtIsNull(tenantId, ownerId, imageAssetType)
                .orElseGet(() -> ImageAssetEntity.builder()
                        .tenantId(tenantId)
                        .ownerId(ownerId)
                        .imageAssetType(imageAssetType)
                        .build());

        String imageUrl = requireImageUrl(imageAssetDTO);
        imageAsset.setOwnerId(ownerId);
        imageAsset.setTenantId(tenantId);
        imageAsset.setImageAssetType(imageAssetType);
        imageAsset.setImageUrl(imageUrl);
        imageAsset.setImageSize(imageAssetDTO.getImageSize());
        imageAsset.setStorageProvider(resolveStorageProvider(imageUrl));
        imageAsset.setObjectKey(extractObjectKey(imageUrl));
        imageAsset.setOriginalFilename(extractOriginalFilename(imageUrl));
        imageAsset.setVisibility(ImageAssetVisibility.PRIVATE);
        imageAsset.setUploadedAt(Instant.now());
        imageAsset.setDeletedAt(null);

        ImageAssetEntity saved = imageAssetRepository.save(imageAsset);
        return new ImageAssetDTO(saved.getImageUrl(), saved.getImageSize());
    }

    @Cacheable(value = "imageAssets", key = "#root.target.buildImageAssetCacheKey(#ownerType, #ownerId)")
    @Override
    public ImageAssetDTO getImageAsset(String ownerType, UUID ownerId) {
        ImageAssetType imageAssetType = resolveImageAssetType(ownerType);
        UUID tenantId = requireTenantId();

        return imageAssetRepository.findByTenantIdAndOwnerIdAndImageAssetTypeAndDeletedAtIsNull(tenantId, ownerId, imageAssetType)
                .map(a -> new ImageAssetDTO(a.getImageUrl(), a.getImageSize()))
                .orElseThrow(() -> new EntityNotFoundException("Image asset not found"));
    }

    private ImageAssetType resolveImageAssetType(String ownerType) {
        if (!StringUtils.hasText(ownerType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ownerType must not be blank");
        }
        try {
            return ImageAssetType.valueOf(ownerType.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported image asset owner type: " + ownerType);
        }
    }

    public String buildImageAssetCacheKey(String ownerType, UUID ownerId) {
        ImageAssetType imageAssetType = resolveImageAssetType(ownerType);
        return buildImageAssetCacheKey(imageAssetType, ownerId);
    }

    public String buildImageAssetCacheKey(ImageAssetType imageAssetType, UUID ownerId) {
        String tenantKey = String.valueOf(TenantContext.getTenantId());
        return tenantKey + ":" + imageAssetType.name() + ":" + ownerId;
    }

    private UUID requireTenantId() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant ID not found in context");
        }
        return tenantId;
    }

    private String requireImageUrl(ImageAssetDTO imageAssetDTO) {
        if (imageAssetDTO == null || !StringUtils.hasText(imageAssetDTO.getImageUrl())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "imageUrl must not be blank");
        }
        return imageAssetDTO.getImageUrl().trim();
    }

    private ImageStorageProvider resolveStorageProvider(String imageUrl) {
        if (imageUrl == null) {
            return ImageStorageProvider.EXTERNAL_URL;
        }
        return imageUrl.contains(".amazonaws.com") || imageUrl.startsWith("s3://")
                ? ImageStorageProvider.S3
                : ImageStorageProvider.EXTERNAL_URL;
    }

    private String extractObjectKey(String imageUrl) {
        try {
            URI uri = URI.create(imageUrl);
            String path = uri.getPath();
            if (!StringUtils.hasText(path)) {
                return null;
            }
            return path.startsWith("/") ? path.substring(1) : path;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String extractOriginalFilename(String imageUrl) {
        String objectKey = extractObjectKey(imageUrl);
        if (!StringUtils.hasText(objectKey)) {
            return null;
        }
        int slashIndex = objectKey.lastIndexOf('/');
        return slashIndex >= 0 ? objectKey.substring(slashIndex + 1) : objectKey;
    }

    private boolean isFallbackEnabled() {
        return s3Endpoint != null && (s3Endpoint.contains("localhost") || s3Endpoint.contains("127.0.0.1") || s3Endpoint.contains("0.0.0.0"));
    }

    private PresignedUrlResponse mockPresignedUrl(String fileName) {
        String objectKey = "test-images/" + UUID.randomUUID() + "_" + fileName;
        return new PresignedUrlResponse(objectKey, "http://localhost/mock-presigned-url");
    }
}
