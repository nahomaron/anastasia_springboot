package com.anastasia.Anastasia_BackEnd.modules.events.mappers;

import com.anastasia.Anastasia_BackEnd.modules.events.model.requests.EventManagerDTO;
import com.anastasia.Anastasia_BackEnd.modules.events.model.EventManagerEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface EventManagerMapper {

    EventManagerEntity eventManagerDTOToEntity(EventManagerDTO eventManagerDTO);

    EventManagerDTO eventManagerEntityToDTO(EventManagerEntity eventManagerEntity);
}
