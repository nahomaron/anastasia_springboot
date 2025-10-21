package com.anastasia.Anastasia_BackEnd.mappers.event;

import com.anastasia.Anastasia_BackEnd.model.event.EventDTO;
import com.anastasia.Anastasia_BackEnd.model.event.EventEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface EventMapper {

    EventEntity eventDTOToEntity(EventDTO eventDTO);

    EventDTO eventEntityToDTO(EventEntity eventEntity);
}
