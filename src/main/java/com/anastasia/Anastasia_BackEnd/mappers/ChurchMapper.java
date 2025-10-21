package com.anastasia.Anastasia_BackEnd.mappers;

import com.anastasia.Anastasia_BackEnd.model.church.ChurchDTO;
import com.anastasia.Anastasia_BackEnd.model.church.ChurchEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ChurchMapper {

    ChurchDTO churchEntityToDTO(ChurchEntity churchEntity);

    @Mapping(target = "churchId", ignore = true)
    @Mapping(target = "churchNumber", ignore = true)
    ChurchEntity churchDTOToEntity(ChurchDTO churchDTO);
}
