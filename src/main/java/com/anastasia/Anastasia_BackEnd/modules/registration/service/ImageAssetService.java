package com.anastasia.Anastasia_BackEnd.modules.registration.service;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.imageasset.ImageAssetDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.imageasset.ImageUploadRequest;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.imageasset.FinalizeImageUploadRequest;
import com.anastasia.Anastasia_BackEnd.common.aws.PresignedUrlResponse;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public interface ImageAssetService {
    PresignedUrlResponse requestPresignedUrl(String ownerType, String ownerId, ImageUploadRequest request);

    ImageAssetDTO saveImageAsset(String ownerType, String ownerId, FinalizeImageUploadRequest request);

    ImageAssetDTO getImageAsset(String ownerType, String ownerId);
}
