package com.anastasia.Anastasia_BackEnd.service.event;

import com.anastasia.Anastasia_BackEnd.mappers.event.EventManagerMapper;
import com.anastasia.Anastasia_BackEnd.mappers.event.EventMapper;
import com.anastasia.Anastasia_BackEnd.model.event.EventDTO;
import com.anastasia.Anastasia_BackEnd.model.event.EventEntity;
import com.anastasia.Anastasia_BackEnd.model.event.requests.EventManagerDTO;
import com.anastasia.Anastasia_BackEnd.model.event.EventManagerEntity;
import com.anastasia.Anastasia_BackEnd.model.user.UserEntity;
import com.anastasia.Anastasia_BackEnd.repository.EventRepository;
import com.anastasia.Anastasia_BackEnd.repository.auth.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService{

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final EventMapper eventMapper;
    private final EventManagerMapper eventManagerMapper;

    @Override
    public EventEntity convertToEntity(EventDTO eventDTO) {
        return eventMapper.eventDTOToEntity(eventDTO);
    }
    @Override
    public EventDTO convertToDTO(EventEntity eventEntity) {
        return eventMapper.eventEntityToDTO(eventEntity);
    }
    @Override
    public EventManagerEntity convertToEntity(EventManagerDTO eventManagerDTO) {
        return eventManagerMapper.eventManagerDTOToEntity(eventManagerDTO);
    }
    @Override
    public EventManagerDTO convertToDTO(EventManagerEntity eventManagerEntity) {
        return eventManagerMapper.eventManagerEntityToDTO(eventManagerEntity);
    }

    @Override
    public void assignManagerToEvent(Long eventId, UUID userId, String role) {

        EventEntity event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found"));
        UserEntity user = userRepository.findById(userId).
                orElseThrow(() -> new EntityNotFoundException("User not found"));

        EventManagerEntity manager = new EventManagerEntity();
        manager.setEvent(event);
        manager.setUser(user);
        manager.setRole(role);
        manager.setAssignedAt(LocalDateTime.now());

        event.getEventManagers().add(manager);
        eventRepository.save(event);
    }

    @Override
    public void removeManager(Long eventId, UUID managerId) {
        EventEntity event = eventRepository.findById(eventId).orElseThrow();
        UserEntity user = userRepository.findById(managerId).orElseThrow();

        EventManagerEntity manager = new EventManagerEntity();
        manager.setEvent(event);
        manager.setUser(user);

        event.getEventManagers().remove(manager);
    }

    @Override
    public List<EventManagerEntity> getManagers(Long eventId) {
        return eventRepository.findAllManagersByEventId(eventId);
    }

    @Override
    public EventEntity createEvent(EventEntity event) {

        if (event.getEventManagers() != null) {
            for (EventManagerEntity manager : event.getEventManagers()) {
                manager.setEvent(event); // 👈 important to set the parent
                manager.setAssignedAt(LocalDateTime.now());
            }
        }
        return eventRepository.save(event);
    }

    @Override
    public EventEntity updateEvent(Long eventId, EventEntity event) {
        if(!eventRepository.existsById(eventId)){
            throw new EntityNotFoundException("Event is not found");
        }
        event.setEventId(eventId);
        return eventRepository.save(event);
    }

    @Override
    public void deleteEvent(Long eventId) {
        if(!eventRepository.existsById(eventId)){
            throw new EntityNotFoundException("Event is not found");
        }
        eventRepository.deleteById(eventId);
    }


}
