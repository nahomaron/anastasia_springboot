package com.anastasia.Anastasia_BackEnd.service.avatar;

import com.anastasia.Anastasia_BackEnd.model.avatar.AvatarDTO;
import com.anastasia.Anastasia_BackEnd.model.avatar.AvatarEntity;
import com.anastasia.Anastasia_BackEnd.model.avatar.AvatarType;
import com.anastasia.Anastasia_BackEnd.model.aws.PresignedUrlResponse;
import com.anastasia.Anastasia_BackEnd.repository.AvatarRepository;
import com.anastasia.Anastasia_BackEnd.service.aws.S3Service;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AvatarServiceImpl implements AvatarService{

    private final S3Service s3Service;
    private final AvatarRepository avatarRepository;

    @Override
    public PresignedUrlResponse requestPresignedUrl(String fileName) {
        return s3Service.generatePresignedUploadUrl(fileName);
    }

    @Override
    public AvatarDTO saveAvatar(String ownerType, UUID ownerId, AvatarDTO avatarDTO) {
        AvatarEntity avatar = AvatarEntity.builder()
                .imageUrl(avatarDTO.getImageUrl())
                .imageSize(avatarDTO.getImageSize())
                .avatarType(AvatarType.valueOf(ownerType.toUpperCase()))
                .build();

        // Set
//        switch (AvatarType.valueOf(ownerType.toUpperCase())) {
//            case USER -> {
//                UserEntity user = userRepository.findById(ownerId)
//                        .orElseThrow(() -> new EntityNotFoundException("User not found"));
//                avatar.setUser(user);
//                user.setProfileAvatar(avatar);
//            }
//            case MEMBER -> {
//                MemberEntity member = memberRepository.findById(ownerId)
//                        .orElseThrow(() -> new EntityNotFoundException("Member not found"));
//                avatar.setMember(member);
//            }
//            // TODO: CHURCH, CHILD, GROUP
//        }

        avatar = avatarRepository.save(avatar);
        return new AvatarDTO(avatar.getImageUrl(), avatar.getImageSize());
    }

    @Override
    public AvatarDTO getAvatar(String ownerType, UUID ownerId) {
        return avatarRepository.findByUserId(ownerId) // for USER case
                .map(a -> new AvatarDTO(a.getImageUrl(), a.getImageSize()))
                .orElseThrow(() -> new EntityNotFoundException("Avatar not found"));
    }


}
