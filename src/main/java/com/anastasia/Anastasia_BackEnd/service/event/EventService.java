package com.anastasia.Anastasia_BackEnd.service.event;

import com.anastasia.Anastasia_BackEnd.model.event.EventDTO;
import com.anastasia.Anastasia_BackEnd.model.event.EventEntity;
import com.anastasia.Anastasia_BackEnd.model.event.requests.EventManagerDTO;
import com.anastasia.Anastasia_BackEnd.model.event.EventManagerEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public interface EventService {

    EventEntity convertToEntity(EventDTO eventDTO);

    EventDTO convertToDTO(EventEntity eventEntity);

    EventManagerEntity convertToEntity(EventManagerDTO eventDTO);

    EventManagerDTO convertToDTO(EventManagerEntity eventEntity);


    void assignManagerToEvent(Long eventId, UUID userId, String role);

    void removeManager(Long eventId, UUID managerId);

    List<EventManagerEntity> getManagers(Long eventId);

    EventEntity createEvent(EventEntity event);

    EventEntity updateEvent(Long eventId, EventEntity event);

    void deleteEvent(Long eventId);
}


