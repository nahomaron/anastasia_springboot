package com.anastasia.Anastasia_BackEnd.service.event;

import com.anastasia.Anastasia_BackEnd.config.TenantContext;
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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
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

    @Cacheable(value = "events_visible", key = "#root.target.visibleEventsCacheKey(#userId)")
    @Override
    public List<EventDTO> getVisibleEventsForUser(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID is required to resolve visible events");
        }

        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant ID not found in context while fetching visible events");
        }

        return eventRepository.findVisibleForUser(tenantId, userId).stream()
                .map(eventMapper::eventEntityToDTO)
                .toList();
    }

    @Caching(evict = {
            @CacheEvict(value = "event_managers", key = "#root.target.eventManagersCacheKey(#eventId)"),
            @CacheEvict(value = "events_visible", allEntries = true)
    })
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

    @Caching(evict = {
            @CacheEvict(value = "event_managers", key = "#root.target.eventManagersCacheKey(#eventId)"),
            @CacheEvict(value = "events_visible", allEntries = true)
    })
    @Override
    public void removeManager(Long eventId, UUID managerId) {
        EventEntity event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found"));
        UserEntity user = userRepository.findById(managerId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (event.getEventManagers() == null || event.getEventManagers().isEmpty()) {
            throw new EntityNotFoundException("Manager not assigned to event");
        }

        boolean removed = event.getEventManagers().removeIf(existingManager -> {
            UserEntity existingUser = existingManager.getUser();
            return existingUser != null && managerId.equals(existingUser.getUuid());
        });

        if (!removed) {
            throw new EntityNotFoundException("Manager not assigned to event");
        }

        eventRepository.save(event);
    }

    @Cacheable(value = "event_managers", key = "#root.target.eventManagersCacheKey(#eventId)")
    @Override
    public List<EventManagerEntity> getManagers(Long eventId) {
        return eventRepository.findAllManagersByEventId(eventId);
    }

    @Caching(evict = {
            @CacheEvict(value = "events_visible", allEntries = true),
            @CacheEvict(value = "event_managers", allEntries = true)
    })
    @Override
    public EventEntity createEvent(EventEntity event) {

        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant ID not found in context while creating event");
        }
        event.setTenantId(tenantId);

        if (event.getEventManagers() != null) {
            for (EventManagerEntity manager : event.getEventManagers()) {
                manager.setEvent(event); // 👈 important to set the parent
                manager.setAssignedAt(LocalDateTime.now());
            }
        }
        return eventRepository.save(event);
    }

    @Caching(evict = {
            @CacheEvict(value = "events_visible", allEntries = true),
            @CacheEvict(value = "event_managers", key = "#root.target.eventManagersCacheKey(#eventId)")
    })
    @Override
    public EventEntity updateEvent(Long eventId, EventEntity event) {
        if(!eventRepository.existsById(eventId)){
            throw new EntityNotFoundException("Event is not found");
        }
        event.setEventId(eventId);
        return eventRepository.save(event);
    }

    @Caching(evict = {
            @CacheEvict(value = "events_visible", allEntries = true),
            @CacheEvict(value = "event_managers", key = "#root.target.eventManagersCacheKey(#eventId)")
    })
    @Override
    public void deleteEvent(Long eventId) {
        if(!eventRepository.existsById(eventId)){
            throw new EntityNotFoundException("Event is not found");
        }
        eventRepository.deleteById(eventId);
    }

    private String visibleEventsCacheKey(UUID userId) {
        return String.valueOf(TenantContext.getTenantId()) + ":visible:" + userId;
    }

    private String eventManagersCacheKey(Long eventId) {
        return String.valueOf(TenantContext.getTenantId()) + ":managers:" + eventId;
    }

}
