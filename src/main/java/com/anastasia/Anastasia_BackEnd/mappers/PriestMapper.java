package com.anastasia.Anastasia_BackEnd.mappers;

import com.anastasia.Anastasia_BackEnd.model.priest.PriestDTO;
import com.anastasia.Anastasia_BackEnd.model.priest.PriestEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PriestMapper {

    PriestDTO priestEntityToDTO(PriestEntity priestEntity);

    PriestEntity priestDTOToEntity(PriestDTO priestDTO);
}
