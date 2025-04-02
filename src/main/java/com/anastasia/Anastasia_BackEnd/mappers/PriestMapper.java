package com.anastasia.Anastasia_BackEnd.mappers;

import com.anastasia.Anastasia_BackEnd.model.priest.PriestDTO;
import com.anastasia.Anastasia_BackEnd.model.priest.PriestEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PriestMapper {

    PriestDTO priestEntityToDTO(PriestEntity priestEntity);

    PriestEntity priestDTOToEntity(PriestDTO priestDTO);
}
