package com.anastasia.Anastasia_BackEnd.api.factories;

import com.anastasia.Anastasia_BackEnd.model.avatar.AvatarDTO;

import java.util.UUID;

/**
 * Factory for creating reusable {@link AvatarDTO} instances for API tests.
 */
public final class AvatarDataFactory {

    private AvatarDataFactory() {
    }

    public static AvatarDTO newValidAvatar() {
        return AvatarDTO.builder()
                .imageUrl("https://cdn.example.com/avatars/" + UUID.randomUUID() + ".png")
                .imageSize("256KB")
                .build();
    }
}
