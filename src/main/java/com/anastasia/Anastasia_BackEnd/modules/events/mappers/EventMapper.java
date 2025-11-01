package com.anastasia.Anastasia_BackEnd.modules.events.mappers;

import com.anastasia.Anastasia_BackEnd.modules.events.model.EventDTO;
import com.anastasia.Anastasia_BackEnd.modules.events.model.EventEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface EventMapper {

    EventEntity eventDTOToEntity(EventDTO eventDTO);

    EventDTO eventEntityToDTO(EventEntity eventEntity);
}
