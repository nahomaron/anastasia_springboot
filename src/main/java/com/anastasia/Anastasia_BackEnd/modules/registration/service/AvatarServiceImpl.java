package com.anastasia.Anastasia_BackEnd.modules.registration.service;

import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.avatar.AvatarDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.avatar.AvatarEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.avatar.AvatarType;
import com.anastasia.Anastasia_BackEnd.common.aws.PresignedUrlResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.AvatarRepository;
import com.anastasia.Anastasia_BackEnd.common.aws.S3Service;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AvatarServiceImpl implements AvatarService{

    private final S3Service s3Service;
    private final AvatarRepository avatarRepository;

    @Override
    public PresignedUrlResponse requestPresignedUrl(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fileName must not be blank");
        }
        return s3Service.generatePresignedUploadUrl(fileName);
    }

    @CachePut(value = "avatars", key = "#root.target.buildAvatarCacheKey(#ownerType, #ownerId)")
    @Override
    public AvatarDTO saveAvatar(String ownerType, UUID ownerId, AvatarDTO avatarDTO) {
        AvatarType avatarType = resolveAvatarType(ownerType);

        AvatarEntity avatar = avatarRepository.findByOwnerIdAndAvatarType(ownerId, avatarType)
                .orElseGet(() -> AvatarEntity.builder()
                        .ownerId(ownerId)
                        .avatarType(avatarType)
                        .build());

        avatar.setOwnerId(ownerId);
        avatar.setAvatarType(avatarType);
        avatar.setImageUrl(avatarDTO.getImageUrl());
        avatar.setImageSize(avatarDTO.getImageSize());

        AvatarEntity saved = avatarRepository.save(avatar);
        return new AvatarDTO(saved.getImageUrl(), saved.getImageSize());
    }

    @Cacheable(value = "avatars", key = "#root.target.buildAvatarCacheKey(#ownerType, #ownerId)")
    @Override
    public AvatarDTO getAvatar(String ownerType, UUID ownerId) {
        AvatarType avatarType = resolveAvatarType(ownerType);

        return avatarRepository.findByOwnerIdAndAvatarType(ownerId, avatarType)
                .map(a -> new AvatarDTO(a.getImageUrl(), a.getImageSize()))
                .orElseThrow(() -> new EntityNotFoundException("Avatar not found"));
    }

    private AvatarType resolveAvatarType(String ownerType) {
        if (!StringUtils.hasText(ownerType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ownerType must not be blank");
        }
        try {
            return AvatarType.valueOf(ownerType.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported avatar owner type: " + ownerType);
        }
    }

    private String buildAvatarCacheKey(String ownerType, UUID ownerId) {
        AvatarType avatarType = resolveAvatarType(ownerType);
        return buildAvatarCacheKey(avatarType, ownerId);
    }

    private String buildAvatarCacheKey(AvatarType avatarType, UUID ownerId) {
        String tenantKey = String.valueOf(TenantContext.getTenantId());
        return tenantKey + ":" + avatarType.name() + ":" + ownerId;
    }

}
