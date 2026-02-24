package com.anastasia.Anastasia_BackEnd.modules.events.service;

import com.anastasia.Anastasia_BackEnd.modules.events.model.EventDTO;
import com.anastasia.Anastasia_BackEnd.modules.events.model.EventEntity;
import com.anastasia.Anastasia_BackEnd.modules.events.model.requests.EventManagerDTO;
import com.anastasia.Anastasia_BackEnd.modules.events.model.EventManagerEntity;
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

    List<EventDTO> getVisibleEventsForUser(UUID userId);

    EventDTO getEventByIdForUser(UUID userId, Long eventId);

    void deleteEvent(Long eventId);
}
