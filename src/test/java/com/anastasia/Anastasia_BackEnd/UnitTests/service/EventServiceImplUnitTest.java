package com.anastasia.Anastasia_BackEnd.UnitTests.service;

import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.modules.events.mappers.EventManagerMapper;
import com.anastasia.Anastasia_BackEnd.modules.events.mappers.EventMapper;
import com.anastasia.Anastasia_BackEnd.modules.events.model.EventDTO;
import com.anastasia.Anastasia_BackEnd.modules.events.model.EventEntity;
import com.anastasia.Anastasia_BackEnd.modules.events.model.EventManagerEntity;
import com.anastasia.Anastasia_BackEnd.modules.events.model.requests.EventManagerDTO;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.modules.events.repository.EventRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import com.anastasia.Anastasia_BackEnd.modules.events.service.EventServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceImplUnitTest {

    @Mock
    private EventRepository eventRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EventMapper eventMapper;
    @Mock
    private EventManagerMapper eventManagerMapper;

    @InjectMocks
    private EventServiceImpl eventService;

    private EventEntity event;
    private UserEntity user;
    private UUID userId;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);

        userId = UUID.randomUUID();
        user = UserEntity.builder().uuid(userId).build();
        event = EventEntity.builder()
                .eventId(7L)
                .eventManagers(new HashSet<>())
                .build();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void getVisibleEventsForUser_whenUserIdMissing_throwsException() {
        assertThatThrownBy(() -> eventService.getVisibleEventsForUser(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User ID is required");
    }

    @Test
    void getVisibleEventsForUser_whenTenantMissing_throwsException() {
        TenantContext.clear();

        assertThatThrownBy(() -> eventService.getVisibleEventsForUser(userId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Tenant ID not found");
    }

    @Test
    void getVisibleEventsForUser_returnsMappedDtosFromRepository() {
        EventEntity visibleEvent = EventEntity.builder().eventId(99L).build();
        EventDTO visibleDto = EventDTO.builder().title("Visible").build();
        user.setEmail("member@example.com");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(eventRepository.findVisibleForUser(tenantId, userId, user.getEmail())).thenReturn(List.of(visibleEvent));
        when(eventMapper.eventEntityToDTO(visibleEvent)).thenReturn(visibleDto);

        List<EventDTO> actual = eventService.getVisibleEventsForUser(userId);

        assertThat(actual).containsExactly(visibleDto);
        verify(eventRepository).findVisibleForUser(tenantId, userId, user.getEmail());
        verify(eventMapper).eventEntityToDTO(visibleEvent);
    }

    @Test
    void assignManagerToEvent_whenValid_addsManagerAndSavesEvent() {
        when(eventRepository.findById(event.getEventId())).thenReturn(Optional.of(event));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        eventService.assignManagerToEvent(event.getEventId(), userId, "ORGANIZER");

        ArgumentCaptor<EventEntity> captor = ArgumentCaptor.forClass(EventEntity.class);
        verify(eventRepository).save(captor.capture());

        EventEntity savedEvent = captor.getValue();
        assertThat(savedEvent.getEventManagers()).hasSize(1);

        EventManagerEntity manager = savedEvent.getEventManagers().iterator().next();
        assertThat(manager.getUser()).isEqualTo(user);
        assertThat(manager.getEvent()).isEqualTo(event);
        assertThat(manager.getRole()).isEqualTo("ORGANIZER");
    }

    @Test
    void assignManagerToEvent_whenEventMissing_throwsException() {
        when(eventRepository.findById(event.getEventId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.assignManagerToEvent(event.getEventId(), userId, "ROLE"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Event not found");

        verify(eventRepository, never()).save(any());
    }

    @Test
    void assignManagerToEvent_whenUserMissing_throwsException() {
        when(eventRepository.findById(event.getEventId())).thenReturn(Optional.of(event));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.assignManagerToEvent(event.getEventId(), userId, "ROLE"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("User not found");

        verify(eventRepository, never()).save(any());
    }

    @Test
    void removeManager_whenManagerExists_removesFromCollection() {
        EventManagerEntity manager = EventManagerEntity.builder()
                .event(event)
                .user(user)
                .build();
        Set<EventManagerEntity> managers = new HashSet<>();
        managers.add(manager);
        event.setEventManagers(managers);

        when(eventRepository.findById(event.getEventId())).thenReturn(Optional.of(event));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        eventService.removeManager(event.getEventId(), userId);

        assertThat(event.getEventManagers()).isEmpty();
    }

    @Test
    void getManagers_delegatesToRepository() {
        eventService.getManagers(event.getEventId());
        verify(eventRepository).findAllManagersByEventId(event.getEventId());
    }

    @Test
    void createEvent_withManagers_setsBackReference() {
        EventManagerEntity manager = EventManagerEntity.builder().build();
        event.getEventManagers().add(manager);

        when(eventRepository.save(event)).thenReturn(event);

        EventEntity result = eventService.createEvent(event);

        assertThat(result.getEventManagers()).hasSize(1);
        EventManagerEntity assignedManager = result.getEventManagers().iterator().next();
        assertThat(assignedManager.getEvent()).isEqualTo(event);
        assertThat(result.getTenantId()).isEqualTo(tenantId);
    }

    @Test
    void createEvent_withoutTenant_throwsException() {
        TenantContext.clear();

        assertThatThrownBy(() -> eventService.createEvent(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Tenant ID not found");

        verify(eventRepository, never()).save(any());
    }

    @Test
    void updateEvent_whenExists_setsIdAndSaves() {
        EventEntity update = EventEntity.builder().build();
        when(eventRepository.existsById(event.getEventId())).thenReturn(true);
        when(eventRepository.save(update)).thenAnswer(invocation -> invocation.getArgument(0));

        EventEntity result = eventService.updateEvent(event.getEventId(), update);

        assertThat(result.getEventId()).isEqualTo(event.getEventId());
        verify(eventRepository).save(update);
    }

    @Test
    void updateEvent_whenMissing_throwsException() {
        when(eventRepository.existsById(event.getEventId())).thenReturn(false);

        assertThatThrownBy(() -> eventService.updateEvent(event.getEventId(), EventEntity.builder().build()))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Event is not found");
    }

    @Test
    void deleteEvent_whenExists_deletesById() {
        when(eventRepository.existsById(event.getEventId())).thenReturn(true);

        eventService.deleteEvent(event.getEventId());

        verify(eventRepository).deleteById(event.getEventId());
    }

    @Test
    void deleteEvent_whenMissing_throwsException() {
        when(eventRepository.existsById(event.getEventId())).thenReturn(false);

        assertThatThrownBy(() -> eventService.deleteEvent(event.getEventId()))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Event is not found");
    }

    @Test
    void convertMethods_delegateToMappers() {
        var eventDTO = com.anastasia.Anastasia_BackEnd.modules.events.model.EventDTO.builder().build();
        var eventEntity = EventEntity.builder().build();
        var managerDTO = EventManagerDTO.builder().build();
        var managerEntity = EventManagerEntity.builder().build();

        eventService.convertToEntity(eventDTO);
        eventService.convertToDTO(eventEntity);
        eventService.convertToEntity(managerDTO);
        eventService.convertToDTO(managerEntity);

        verify(eventMapper).eventDTOToEntity(eventDTO);
        verify(eventMapper).eventEntityToDTO(eventEntity);
        verify(eventManagerMapper).eventManagerDTOToEntity(managerDTO);
        verify(eventManagerMapper).eventManagerEntityToDTO(managerEntity);
    }
}
