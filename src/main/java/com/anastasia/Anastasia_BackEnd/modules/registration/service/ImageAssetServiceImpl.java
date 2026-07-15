package com.anastasia.Anastasia_BackEnd.modules.registration.service;

import com.anastasia.Anastasia_BackEnd.common.aws.PresignedUrlResponse;
import com.anastasia.Anastasia_BackEnd.common.aws.S3Service;
import com.anastasia.Anastasia_BackEnd.common.aws.StoredObjectMetadata;
import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.core.auth.principal.UserPrincipal;
import com.anastasia.Anastasia_BackEnd.modules.events.model.EventEntity;
import com.anastasia.Anastasia_BackEnd.modules.events.repository.EventRepository;
import com.anastasia.Anastasia_BackEnd.modules.groups.GroupRepository;
import com.anastasia.Anastasia_BackEnd.modules.groups.model.GroupEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.imageasset.FinalizeImageUploadRequest;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.imageasset.ImageAssetDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.imageasset.ImageAssetEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.imageasset.ImageAssetType;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.imageasset.ImageAssetUploadIntentEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.imageasset.ImageAssetVisibility;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.imageasset.ImageStorageProvider;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.imageasset.ImageUploadRequest;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ImageAssetRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ImageAssetUploadIntentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageAssetServiceImpl implements ImageAssetService {

    private static final long UPLOAD_TTL_SECONDS = 15 * 60L;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );
    private static final Map<String, String> CONTENT_TYPE_EXTENSIONS = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp"
    );
    private static final Set<String> ELEVATED_AUTHORITIES = Set.of(
            "MANAGE_TENANTS",
            "MANAGE_STAFF"
    );
    private static final Set<String> USER_READ_AUTHORITIES = Set.of(
            "VIEW_TENANT_USERS",
            "MANAGE_TENANT_USERS",
            "MANAGE_USERS",
            "VIEW_ALL_DATA",
            "MANAGE_TENANTS"
    );
    private static final Set<String> USER_WRITE_AUTHORITIES = Set.of(
            "MANAGE_TENANT_USERS",
            "MANAGE_USERS",
            "MANAGE_TENANTS"
    );
    private static final Set<String> MEMBER_READ_AUTHORITIES = Set.of(
            "VIEW_MEMBERS",
            "MANAGE_MEMBERS",
            "VIEW_ALL_DATA",
            "MANAGE_TENANTS"
    );
    private static final Set<String> MEMBER_WRITE_AUTHORITIES = Set.of(
            "EDIT_MEMBERS",
            "MANAGE_MEMBERS",
            "MANAGE_TENANTS"
    );
    private static final Set<String> CHILD_READ_AUTHORITIES = Set.of(
            "VIEW_CHILDREN",
            "MANAGE_MEMBERS",
            "VIEW_ALL_DATA",
            "MANAGE_TENANTS"
    );
    private static final Set<String> CHILD_WRITE_AUTHORITIES = Set.of(
            "EDIT_CHILDREN",
            "MANAGE_MEMBERS",
            "MANAGE_TENANTS"
    );
    private static final Set<String> CHURCH_READ_AUTHORITIES = Set.of(
            "OWN_SUBSCRIPTION",
            "MANAGE_TENANT_BILLING",
            "VIEW_ALL_DATA",
            "MANAGE_TENANTS"
    );
    private static final Set<String> CHURCH_WRITE_AUTHORITIES = Set.of(
            "OWN_SUBSCRIPTION",
            "MANAGE_TENANT_BILLING",
            "MANAGE_TENANTS"
    );
    private static final Set<String> GROUP_READ_AUTHORITIES = Set.of(
            "VIEW_GROUPS",
            "MANAGE_GROUPS",
            "VIEW_ALL_DATA",
            "MANAGE_TENANTS"
    );
    private static final Set<String> GROUP_WRITE_AUTHORITIES = Set.of(
            "CREATE_GROUPS",
            "EDIT_GROUPS",
            "MANAGE_GROUPS",
            "MANAGE_TENANTS"
    );
    private static final Set<String> EVENT_READ_AUTHORITIES = Set.of(
            "VIEW_EVENTS",
            "VIEW_EVENT_REPORTS",
            "MANAGE_EVENTS",
            "VIEW_ALL_DATA",
            "MANAGE_TENANTS"
    );
    private static final Set<String> EVENT_WRITE_AUTHORITIES = Set.of(
            "CREATE_EDIT_EVENTS",
            "MANAGE_EVENTS",
            "MANAGE_CALENDAR",
            "MANAGE_TENANTS"
    );

    private final S3Service s3Service;
    private final ImageAssetRepository imageAssetRepository;
    private final ImageAssetUploadIntentRepository uploadIntentRepository;
    private final EventRepository eventRepository;
    private final GroupRepository groupRepository;
    private final Environment environment;

    @Value("${aws.s3.endpoint:http://localhost:4566}")
    private String s3Endpoint;

    @Override
    public PresignedUrlResponse requestPresignedUrl(String ownerType, String ownerId, ImageUploadRequest request) {
        ImageAssetType imageAssetType = resolveImageAssetType(ownerType);
        UUID tenantId = requireTenantId();
        UUID currentUserId = requireCurrentUserId();
        UUID resolvedOwnerId = resolveAndAuthorizeOwner(imageAssetType, ownerId, tenantId, currentUserId, ImageAssetAccess.WRITE);
        String normalizedContentType = normalizeAndValidateContentType(request);
        String sanitizedFileName = sanitizeFilename(request.getFileName());
        String objectKey = buildObjectKey(tenantId, imageAssetType, ownerId, normalizedContentType);

        ImageAssetUploadIntentEntity intent = uploadIntentRepository.save(ImageAssetUploadIntentEntity.builder()
                .tenantId(tenantId)
                .ownerId(resolvedOwnerId)
                .imageAssetType(imageAssetType)
                .objectKey(objectKey)
                .originalFilename(sanitizedFileName)
                .contentType(normalizedContentType)
                .fileSizeBytes(request.getFileSizeBytes())
                .uploadedByUserId(currentUserId)
                .expiresAt(Instant.now().plusSeconds(UPLOAD_TTL_SECONDS))
                .build());

        try {
            PresignedUrlResponse response = s3Service.generatePresignedUploadUrl(objectKey, normalizedContentType);
            response.setUploadId(intent.getId());
            response.setObjectKey(objectKey);
            response.setObjectUrl(s3Service.buildObjectUrl(objectKey));
            response.setContentType(normalizedContentType);
            return response;
        } catch (RuntimeException ex) {
            uploadIntentRepository.deleteById(intent.getId());
            if (isFallbackEnabled()) {
                log.warn("Unable to generate presigned URL via S3 ({}). Falling back to mock response.", ex.getMessage());
                return mockPresignedUrl(intent.getId(), objectKey, normalizedContentType);
            }
            throw ex;
        }
    }

    @CachePut(value = "imageAssets", key = "#root.target.buildImageAssetCacheKey(#ownerType, #ownerId)")
    @Override
    public ImageAssetDTO saveImageAsset(String ownerType, String ownerId, FinalizeImageUploadRequest request) {
        ImageAssetType imageAssetType = resolveImageAssetType(ownerType);
        UUID tenantId = requireTenantId();
        UUID currentUserId = requireCurrentUserId();
        UUID resolvedOwnerId = resolveAndAuthorizeOwner(imageAssetType, ownerId, tenantId, currentUserId, ImageAssetAccess.WRITE);

        ImageAssetUploadIntentEntity intent = uploadIntentRepository.findByIdAndTenantId(request.getUploadId(), tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Upload intent not found"));
        validateIntent(intent, imageAssetType, resolvedOwnerId, currentUserId);

        StoredObjectMetadata metadata = s3Service.verifyUploadedObject(intent.getObjectKey());
        validateStoredObject(intent, metadata);

        ImageAssetEntity imageAsset = imageAssetRepository
                .findByTenantIdAndOwnerIdAndImageAssetTypeAndDeletedAtIsNull(tenantId, resolvedOwnerId, imageAssetType)
                .orElseGet(() -> ImageAssetEntity.builder()
                        .tenantId(tenantId)
                        .ownerId(resolvedOwnerId)
                        .imageAssetType(imageAssetType)
                        .build());

        imageAsset.setOwnerId(resolvedOwnerId);
        imageAsset.setTenantId(tenantId);
        imageAsset.setImageAssetType(imageAssetType);
        imageAsset.setImageUrl(metadata.objectUrl());
        imageAsset.setImageSize(trimToNull(request.getImageSize()));
        imageAsset.setStorageProvider(ImageStorageProvider.S3);
        imageAsset.setObjectKey(metadata.objectKey());
        imageAsset.setOriginalFilename(intent.getOriginalFilename());
        imageAsset.setContentType(metadata.contentType());
        imageAsset.setFileSizeBytes(metadata.fileSizeBytes());
        imageAsset.setUploadedByUserId(currentUserId);
        imageAsset.setVisibility(ImageAssetVisibility.PRIVATE);
        imageAsset.setUploadedAt(Instant.now());
        imageAsset.setDeletedAt(null);

        intent.setCompletedAt(Instant.now());
        uploadIntentRepository.save(intent);

        ImageAssetEntity saved = imageAssetRepository.save(imageAsset);
        return new ImageAssetDTO(saved.getImageUrl(), saved.getImageSize());
    }

    @Cacheable(value = "imageAssets", key = "#root.target.buildImageAssetCacheKey(#ownerType, #ownerId)")
    @Override
    public ImageAssetDTO getImageAsset(String ownerType, String ownerId) {
        ImageAssetType imageAssetType = resolveImageAssetType(ownerType);
        UUID tenantId = requireTenantId();
        UUID resolvedOwnerId = resolveAndAuthorizeOwner(imageAssetType, ownerId, tenantId, requireCurrentUserId(), ImageAssetAccess.READ);

        return imageAssetRepository.findByTenantIdAndOwnerIdAndImageAssetTypeAndDeletedAtIsNull(tenantId, resolvedOwnerId, imageAssetType)
                .map(a -> new ImageAssetDTO(a.getImageUrl(), a.getImageSize()))
                .orElseThrow(() -> new EntityNotFoundException("Image asset not found"));
    }

    private ImageAssetType resolveImageAssetType(String ownerType) {
        if (!StringUtils.hasText(ownerType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ownerType must not be blank");
        }
        try {
            return ImageAssetType.valueOf(ownerType.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported image asset owner type: " + ownerType);
        }
    }

    public String buildImageAssetCacheKey(String ownerType, String ownerId) {
        ImageAssetType imageAssetType = resolveImageAssetType(ownerType);
        return buildImageAssetCacheKey(imageAssetType, ownerId);
    }

    public String buildImageAssetCacheKey(ImageAssetType imageAssetType, String ownerId) {
        String tenantKey = String.valueOf(TenantContext.getTenantId());
        return tenantKey + ":" + imageAssetType.name() + ":" + ownerId;
    }

    private UUID resolveAndAuthorizeOwner(
            ImageAssetType imageAssetType,
            String ownerId,
            UUID tenantId,
            UUID currentUserId,
            ImageAssetAccess access
    ) {
        String normalizedOwnerId = trimToNull(ownerId);
        if (normalizedOwnerId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ownerId must not be blank");
        }

        if (imageAssetType == ImageAssetType.USER) {
            UUID userOwnerId = parseUuidOwnerId(normalizedOwnerId);
            Set<String> allowedAuthorities = access == ImageAssetAccess.READ ? USER_READ_AUTHORITIES : USER_WRITE_AUTHORITIES;
            if (!userOwnerId.equals(currentUserId) && !currentUserHasAnyAuthority(allowedAuthorities)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot access images for another user.");
            }
            return userOwnerId;
        }

        if (imageAssetType == ImageAssetType.CHURCH) {
            if (!currentUserHasAnyAuthority(access == ImageAssetAccess.READ ? CHURCH_READ_AUTHORITIES : CHURCH_WRITE_AUTHORITIES)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This image asset type requires tenant profile privileges.");
            }
            return parseUuidOwnerId(normalizedOwnerId);
        }

        if (imageAssetType == ImageAssetType.MEMBER) {
            if (!currentUserHasAnyAuthority(access == ImageAssetAccess.READ ? MEMBER_READ_AUTHORITIES : MEMBER_WRITE_AUTHORITIES)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This image asset type requires member privileges.");
            }
            return parseUuidOwnerId(normalizedOwnerId);
        }

        if (imageAssetType == ImageAssetType.CHILD) {
            if (!currentUserHasAnyAuthority(access == ImageAssetAccess.READ ? CHILD_READ_AUTHORITIES : CHILD_WRITE_AUTHORITIES)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This image asset type requires child member privileges.");
            }
            return parseUuidOwnerId(normalizedOwnerId);
        }

        if (imageAssetType == ImageAssetType.GROUP) {
            if (!currentUserHasAnyAuthority(access == ImageAssetAccess.READ ? GROUP_READ_AUTHORITIES : GROUP_WRITE_AUTHORITIES)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This image asset type requires group privileges.");
            }
            Long groupId = parseLongOwnerId(normalizedOwnerId, imageAssetType);
            GroupEntity group = groupRepository.findByGroupIdAndTenantId(groupId, tenantId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
            return derivedOwnerKey(imageAssetType, tenantId, String.valueOf(group.getGroupId()));
        }

        if (imageAssetType == ImageAssetType.EVENT) {
            if (!currentUserHasAnyAuthority(access == ImageAssetAccess.READ ? EVENT_READ_AUTHORITIES : EVENT_WRITE_AUTHORITIES)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This image asset type requires event privileges.");
            }
            Long eventId = parseLongOwnerId(normalizedOwnerId, imageAssetType);
            EventEntity event = eventRepository.findById(eventId)
                    .filter(existing -> tenantId.equals(existing.getTenantId()))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
            return derivedOwnerKey(imageAssetType, tenantId, String.valueOf(event.getEventId()));
        }

        if (!currentUserHasElevatedAuthority()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This image asset type requires elevated privileges.");
        }
        return parseUuidOwnerId(normalizedOwnerId);
    }

    private void validateIntent(ImageAssetUploadIntentEntity intent, ImageAssetType imageAssetType, UUID ownerId, UUID currentUserId) {
        if (intent.getCompletedAt() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Upload intent has already been completed.");
        }
        if (intent.getExpiresAt() == null || !intent.getExpiresAt().isAfter(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.GONE, "Upload intent has expired.");
        }
        if (!intent.getOwnerId().equals(ownerId) || intent.getImageAssetType() != imageAssetType) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Upload intent does not match the target image asset.");
        }
        if (!intent.getUploadedByUserId().equals(currentUserId)
                && !currentUserCanAccessImageType(imageAssetType, ImageAssetAccess.WRITE)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot complete another user's upload intent.");
        }
    }

    private void validateStoredObject(ImageAssetUploadIntentEntity intent, StoredObjectMetadata metadata) {
        if (!intent.getObjectKey().equals(metadata.objectKey())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uploaded object key does not match the upload intent.");
        }
        if (!intent.getContentType().equalsIgnoreCase(metadata.contentType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uploaded object content type does not match the upload intent.");
        }
        if (metadata.fileSizeBytes() <= 0 || metadata.fileSizeBytes() > intent.getFileSizeBytes()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uploaded object size does not match the upload intent.");
        }
    }

    private String normalizeAndValidateContentType(ImageUploadRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Upload request must not be empty");
        }
        String normalizedContentType = trimToNull(request.getContentType());
        if (normalizedContentType == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "contentType must not be blank");
        }
        normalizedContentType = normalizedContentType.toLowerCase(Locale.ROOT);
        if (!ALLOWED_CONTENT_TYPES.contains(normalizedContentType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported image content type: " + normalizedContentType);
        }
        if (request.getFileSizeBytes() == null || request.getFileSizeBytes() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fileSizeBytes must be positive");
        }
        return normalizedContentType;
    }

    private String buildObjectKey(UUID tenantId, ImageAssetType imageAssetType, UUID ownerId, String contentType) {
        String extension = CONTENT_TYPE_EXTENSIONS.getOrDefault(contentType, "bin");
        return "tenant/" + tenantId + "/image-assets/" + imageAssetType.name().toLowerCase(Locale.ROOT) + "/" + ownerId + "/" + UUID.randomUUID() + "." + extension;
    }

    private String buildObjectKey(UUID tenantId, ImageAssetType imageAssetType, String ownerId, String contentType) {
        String extension = CONTENT_TYPE_EXTENSIONS.getOrDefault(contentType, "bin");
        return "tenant/" + tenantId + "/image-assets/" + imageAssetType.name().toLowerCase(Locale.ROOT) + "/" + ownerId + "/" + UUID.randomUUID() + "." + extension;
    }

    private UUID parseUuidOwnerId(String ownerId) {
        try {
            return UUID.fromString(ownerId);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ownerId must be a valid UUID");
        }
    }

    private Long parseLongOwnerId(String ownerId, ImageAssetType imageAssetType) {
        try {
            return Long.valueOf(ownerId);
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "ownerId must be a numeric identifier for " + imageAssetType.name().toLowerCase(Locale.ROOT)
            );
        }
    }

    private UUID derivedOwnerKey(ImageAssetType imageAssetType, UUID tenantId, String ownerId) {
        String source = imageAssetType.name() + ":" + tenantId + ":" + ownerId;
        return UUID.nameUUIDFromBytes(source.getBytes(StandardCharsets.UTF_8));
    }

    private String sanitizeFilename(String fileName) {
        String trimmed = trimToNull(fileName);
        if (trimmed == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fileName must not be blank");
        }
        return trimmed.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private UUID requireTenantId() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant ID not found in context");
        }
        return tenantId;
    }

    private UUID requireCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal userPrincipal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No authenticated user found.");
        }
        return userPrincipal.getUserUuid();
    }

    private boolean currentUserHasElevatedAuthority() {
        return currentUserHasAnyAuthority(ELEVATED_AUTHORITIES);
    }

    private boolean currentUserCanAccessImageType(ImageAssetType imageAssetType, ImageAssetAccess access) {
        return switch (imageAssetType) {
            case USER -> currentUserHasAnyAuthority(access == ImageAssetAccess.READ ? USER_READ_AUTHORITIES : USER_WRITE_AUTHORITIES);
            case MEMBER -> currentUserHasAnyAuthority(access == ImageAssetAccess.READ ? MEMBER_READ_AUTHORITIES : MEMBER_WRITE_AUTHORITIES);
            case CHILD -> currentUserHasAnyAuthority(access == ImageAssetAccess.READ ? CHILD_READ_AUTHORITIES : CHILD_WRITE_AUTHORITIES);
            case CHURCH -> currentUserHasAnyAuthority(access == ImageAssetAccess.READ ? CHURCH_READ_AUTHORITIES : CHURCH_WRITE_AUTHORITIES);
            case GROUP -> currentUserHasAnyAuthority(access == ImageAssetAccess.READ ? GROUP_READ_AUTHORITIES : GROUP_WRITE_AUTHORITIES);
            case EVENT -> currentUserHasAnyAuthority(access == ImageAssetAccess.READ ? EVENT_READ_AUTHORITIES : EVENT_WRITE_AUTHORITIES);
        };
    }

    private boolean currentUserHasAnyAuthority(Set<String> allowedAuthorities) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(granted -> granted.getAuthority())
                .anyMatch(allowedAuthorities::contains);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean isFallbackEnabled() {
        return isDevProfileActive()
                && s3Endpoint != null
                && (s3Endpoint.contains("localhost")
                || s3Endpoint.contains("127.0.0.1")
                || s3Endpoint.contains("0.0.0.0"));
    }

    private boolean isDevProfileActive() {
        for (String activeProfile : environment.getActiveProfiles()) {
            if ("dev".equalsIgnoreCase(activeProfile)) {
                return true;
            }
        }
        return false;
    }

    private PresignedUrlResponse mockPresignedUrl(UUID uploadId, String objectKey, String contentType) {
        return new PresignedUrlResponse(
                uploadId,
                objectKey,
                s3Service.buildObjectUrl(objectKey),
                "http://localhost/mock-presigned-url",
                contentType
        );
    }

    private enum ImageAssetAccess {
        READ,
        WRITE
    }
}
