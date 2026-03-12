package com.anastasia.Anastasia_BackEnd.modules.registration.model.imageasset;

import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(
        name = "image_assets",
        indexes = {
                @Index(name = "idx_image_assets_tenant_owner_type", columnList = "tenant_id, owner_id, image_asset_type"),
                @Index(name = "idx_image_assets_uploaded_by", columnList = "uploaded_by_user_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_image_assets_tenant_owner_type",
                        columnNames = {"tenant_id", "owner_id", "image_asset_type"}
                )
        }
)
public class ImageAssetEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "image_asset_type", nullable = false)
    private ImageAssetType imageAssetType;

    @Enumerated(EnumType.STRING)
    @Column(name = "storage_provider", nullable = false, length = 24)
    @Builder.Default
    private ImageStorageProvider storageProvider = ImageStorageProvider.EXTERNAL_URL;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @Column(name = "image_size")
    private String imageSize;

    @Column(name = "object_key", length = 512)
    private String objectKey;

    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    @Column(name = "content_type", length = 128)
    private String contentType;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    private Integer width;

    private Integer height;

    @Column(length = 128)
    private String checksum;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    @Builder.Default
    private ImageAssetVisibility visibility = ImageAssetVisibility.PRIVATE;

    @Column(name = "uploaded_by_user_id")
    private UUID uploadedByUserId;

    @Column(name = "uploaded_at")
    private Instant uploadedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    @Column(nullable = false)
    private long version;

    @PrePersist
    public void onCreate() {
        if (tenantId == null) {
            tenantId = TenantContext.getTenantId();
        }
        if (uploadedAt == null) {
            uploadedAt = Instant.now();
        }
        if (storageProvider == null) {
            storageProvider = ImageStorageProvider.EXTERNAL_URL;
        }
        if (visibility == null) {
            visibility = ImageAssetVisibility.PRIVATE;
        }
    }

}
