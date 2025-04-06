package com.anastasia.Anastasia_BackEnd.mappers.event;

import com.anastasia.Anastasia_BackEnd.model.event.EventDTO;
import com.anastasia.Anastasia_BackEnd.model.event.EventEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EventMapper {

    EventEntity eventDTOToEntity(EventDTO eventDTO);

    EventDTO eventEntityToDTO(EventEntity eventEntity);
}
