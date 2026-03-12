package com.anastasia.Anastasia_BackEnd.Api.factories;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.imageasset.ImageAssetDTO;

import java.util.UUID;

/**
 * Factory for creating reusable {@link ImageAssetDTO} instances for API tests.
 */
public final class AvatarDataFactory {

    private AvatarDataFactory() {
    }

    public static ImageAssetDTO newValidAvatar() {
        return ImageAssetDTO.builder()
                .imageUrl("https://cdn.example.com/imageAssets/" + UUID.randomUUID() + ".png")
                .imageSize("256KB")
                .build();
    }
}
