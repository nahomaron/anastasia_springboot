package com.anastasia.Anastasia_BackEnd.modules.registration.repository;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.imageasset.ImageAssetUploadIntentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ImageAssetUploadIntentRepository extends JpaRepository<ImageAssetUploadIntentEntity, UUID> {

    Optional<ImageAssetUploadIntentEntity> findByIdAndTenantId(UUID id, UUID tenantId);
}
