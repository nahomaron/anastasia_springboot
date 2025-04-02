package com.anastasia.Anastasia_BackEnd.mappers;

import com.anastasia.Anastasia_BackEnd.model.church.ChurchDTO;
import com.anastasia.Anastasia_BackEnd.model.church.ChurchEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ChurchMapper {

    ChurchDTO churchEntityToDTO(ChurchEntity churchEntity);

    ChurchEntity churchDTOToEntity(ChurchDTO churchDTO);
}
