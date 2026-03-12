package com.anastasia.Anastasia_BackEnd.modules.registration.service;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.imageasset.ImageAssetDTO;
import com.anastasia.Anastasia_BackEnd.common.aws.PresignedUrlResponse;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public interface ImageAssetService {
    PresignedUrlResponse requestPresignedUrl(String fileName);

    ImageAssetDTO saveImageAsset(String ownerType, UUID ownerId, ImageAssetDTO imageAssetDTO);

    ImageAssetDTO getImageAsset(String ownerType, UUID ownerId);
}
