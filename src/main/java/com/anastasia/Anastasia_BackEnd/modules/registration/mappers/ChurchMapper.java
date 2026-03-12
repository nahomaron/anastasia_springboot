package com.anastasia.Anastasia_BackEnd.modules.registration.mappers;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.imageasset.ImageAssetDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.imageasset.ImageAssetEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ChurchMapper {

    default ChurchDTO churchEntityToDTO(ChurchEntity churchEntity) {
        if (churchEntity == null) {
            return null;
        }

        return ChurchDTO.builder()
                .prefix(churchEntity.getPrefix())
                .tPrefix(churchEntity.getTPrefix())
                .churchName(churchEntity.getChurchName())
                .tChurchName(churchEntity.getTChurchName())
                .neighborhood(churchEntity.getNeighborhood())
                .tNeighborhood(churchEntity.getTNeighborhood())
                .diocese(churchEntity.getDiocese())
                .address(churchEntity.getAddress())
                .email(churchEntity.getEmail())
                .phone(churchEntity.getPhone())
                .timezone(churchEntity.getTimezone())
                .locale(churchEntity.getLocale())
                .denomination(churchEntity.getDenomination())
                .description(churchEntity.getDescription())
                .usesOurServices(churchEntity.isUsesOurServices())
                .gpsLocation(churchEntity.getGpsLocation())
                .latitude(churchEntity.getLatitude())
                .longitude(churchEntity.getLongitude())
                .website(churchEntity.getWebsite())
                .instagram(churchEntity.getInstagram())
                .youtube(churchEntity.getYoutube())
                .facebook(churchEntity.getFacebook())
                .churchProfileComplete(churchEntity.isChurchProfileComplete())
                .status(churchEntity.getStatus())
                .profilePicture(map(churchEntity.getProfilePicture()))
                .build();
    }

    default ChurchResponse churchEntityToResponse(ChurchEntity churchEntity) {
        if (churchEntity == null) {
            return null;
        }

        return ChurchResponse.builder()
                .churchId(churchEntity.getChurchId())
                .churchNumber(churchEntity.getChurchNumber())
                .prefix(churchEntity.getPrefix())
                .tPrefix(churchEntity.getTPrefix())
                .profilePicture(map(churchEntity.getProfilePicture()))
                .churchName(churchEntity.getChurchName())
                .tChurchName(churchEntity.getTChurchName())
                .neighborhood(churchEntity.getNeighborhood())
                .tNeighborhood(churchEntity.getTNeighborhood())
                .diocese(churchEntity.getDiocese())
                .address(churchEntity.getAddress())
                .email(churchEntity.getEmail())
                .phone(churchEntity.getPhone())
                .timezone(churchEntity.getTimezone())
                .locale(churchEntity.getLocale())
                .denomination(churchEntity.getDenomination())
                .description(churchEntity.getDescription())
                .usesOurServices(churchEntity.isUsesOurServices())
                .gpsLocation(churchEntity.getGpsLocation())
                .latitude(churchEntity.getLatitude())
                .longitude(churchEntity.getLongitude())
                .website(churchEntity.getWebsite())
                .instagram(churchEntity.getInstagram())
                .youtube(churchEntity.getYoutube())
                .facebook(churchEntity.getFacebook())
                .churchProfileComplete(churchEntity.isChurchProfileComplete())
                .status(churchEntity.getStatus())
                .build();
    }

    default ChurchEntity churchDTOToEntity(ChurchDTO churchDTO) {
        if (churchDTO == null) {
            return null;
        }

        return ChurchEntity.builder()
                .prefix(churchDTO.getPrefix())
                .tPrefix(churchDTO.getTPrefix())
                .churchName(churchDTO.getChurchName())
                .tChurchName(churchDTO.getTChurchName())
                .neighborhood(churchDTO.getNeighborhood())
                .tNeighborhood(churchDTO.getTNeighborhood())
                .diocese(churchDTO.getDiocese())
                .address(churchDTO.getAddress())
                .email(churchDTO.getEmail())
                .phone(churchDTO.getPhone())
                .timezone(churchDTO.getTimezone())
                .locale(churchDTO.getLocale())
                .denomination(churchDTO.getDenomination())
                .description(churchDTO.getDescription())
                .usesOurServices(churchDTO.isUsesOurServices())
                .gpsLocation(churchDTO.getGpsLocation())
                .latitude(churchDTO.getLatitude())
                .longitude(churchDTO.getLongitude())
                .website(churchDTO.getWebsite())
                .instagram(churchDTO.getInstagram())
                .youtube(churchDTO.getYoutube())
                .facebook(churchDTO.getFacebook())
                .churchProfileComplete(churchDTO.isChurchProfileComplete())
                .status(churchDTO.getStatus())
                .profilePicture(map(churchDTO.getProfilePicture()))
                .build();
    }

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
