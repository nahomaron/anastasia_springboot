package com.anastasia.Anastasia_BackEnd.modules.registration.service;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.avatar.AvatarDTO;
import com.anastasia.Anastasia_BackEnd.common.aws.PresignedUrlResponse;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public interface AvatarService {
    PresignedUrlResponse requestPresignedUrl(String fileName);

    AvatarDTO saveAvatar(String ownerType, UUID ownerId, AvatarDTO avatarDTO);

    AvatarDTO getAvatar(String ownerType, UUID ownerId);
}
