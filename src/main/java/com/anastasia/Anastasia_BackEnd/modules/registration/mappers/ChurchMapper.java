package com.anastasia.Anastasia_BackEnd.modules.registration.mappers;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.avatar.AvatarDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.avatar.AvatarEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ChurchMapper {

    ChurchDTO churchEntityToDTO(ChurchEntity churchEntity);

    ChurchResponse churchEntityToResponse(ChurchEntity churchEntity);

    @Mapping(target = "churchId", ignore = true)
    @Mapping(target = "churchNumber", ignore = true)
    ChurchEntity churchDTOToEntity(ChurchDTO churchDTO);

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
