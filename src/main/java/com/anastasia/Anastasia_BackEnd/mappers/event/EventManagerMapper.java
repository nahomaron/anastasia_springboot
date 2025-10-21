package com.anastasia.Anastasia_BackEnd.mappers.event;

import com.anastasia.Anastasia_BackEnd.model.event.requests.EventManagerDTO;
import com.anastasia.Anastasia_BackEnd.model.event.EventManagerEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface EventManagerMapper {

    EventManagerEntity eventManagerDTOToEntity(EventManagerDTO eventManagerDTO);

    EventManagerDTO eventManagerEntityToDTO(EventManagerEntity eventManagerEntity);
}
