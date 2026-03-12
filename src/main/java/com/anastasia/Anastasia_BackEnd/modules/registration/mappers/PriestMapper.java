package com.anastasia.Anastasia_BackEnd.modules.registration.mappers;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.imageasset.ImageAssetDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.imageasset.ImageAssetEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.priest.PriestDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.priest.PriestEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.priest.PriestResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PriestMapper {

    PriestDTO priestEntityToDTO(PriestEntity priestEntity);

    PriestResponse priestEntityToResponse(PriestEntity priestEntity);

    PriestEntity priestDTOToEntity(PriestDTO priestDTO);

    default ImageAssetEntity map(ImageAssetDTO avatar) {
        if (avatar == null) {
            return null;
        }
        return ImageAssetEntity.builder()
                .imageUrl(avatar.getImageUrl())
                .imageSize(avatar.getImageSize())
                .build();
    }

    default ImageAssetDTO map(ImageAssetEntity avatar) {
        if (avatar == null) {
            return null;
        }
        return ImageAssetDTO.builder()
                .imageUrl(avatar.getImageUrl())
                .imageSize(avatar.getImageSize())
                .build();
    }
}
