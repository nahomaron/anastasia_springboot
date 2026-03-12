package com.anastasia.Anastasia_BackEnd.modules.registration.repository;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.imageasset.ImageAssetEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.imageasset.ImageAssetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ImageAssetRepository extends JpaRepository<ImageAssetEntity, Long> {

    Optional<ImageAssetEntity> findByOwnerId(UUID userId);

    Optional<ImageAssetEntity> findByOwnerIdAndImageAssetType(UUID ownerId, ImageAssetType imageAssetType);

    Optional<ImageAssetEntity> findByTenantIdAndOwnerIdAndImageAssetTypeAndDeletedAtIsNull(
            UUID tenantId,
            UUID ownerId,
            ImageAssetType imageAssetType
    );

}
