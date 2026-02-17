package com.anastasia.Anastasia_BackEnd.modules.registration.mappers;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.avatar.AvatarDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.avatar.AvatarEntity;
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

    default AvatarEntity map(AvatarDTO avatar) {
        if (avatar == null) {
            return null;
        }
        return AvatarEntity.builder()
                .imageUrl(avatar.getImageUrl())
                .imageSize(avatar.getImageSize())
                .build();
    }

    default AvatarDTO map(AvatarEntity avatar) {
        if (avatar == null) {
            return null;
        }
        return AvatarDTO.builder()
                .imageUrl(avatar.getImageUrl())
                .imageSize(avatar.getImageSize())
                .build();
    }
}
