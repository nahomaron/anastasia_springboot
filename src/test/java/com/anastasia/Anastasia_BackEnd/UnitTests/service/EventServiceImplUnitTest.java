package com.anastasia.Anastasia_BackEnd.UnitTests.service;

import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.common.i18n.LocalizedMessageService;
import com.anastasia.Anastasia_BackEnd.modules.events.mappers.EventManagerMapper;
import com.anastasia.Anastasia_BackEnd.modules.events.mappers.EventMapper;
import com.anastasia.Anastasia_BackEnd.modules.events.model.EventDTO;
import com.anastasia.Anastasia_BackEnd.modules.events.model.EventEntity;
import com.anastasia.Anastasia_BackEnd.modules.events.model.EventManagerEntity;
import com.anastasia.Anastasia_BackEnd.modules.events.model.requests.EventManagerDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.Adult_MemberEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChurchRepository;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.modules.events.repository.EventRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import com.anastasia.Anastasia_BackEnd.modules.events.service.EventServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import com.anastasia.Anastasia_BackEnd.UnitTests.support.LenientMockitoTest;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@LenientMockitoTest
class EventServiceImplUnitTest {

    @Mock
    private EventRepository eventRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EventMapper eventMapper;
    @Mock
    private EventManagerMapper eventManagerMapper;
    @Mock
    private LocalizedMessageService messageService;
    @Mock
    private ChurchRepository churchRepository;

    @InjectMocks
    private EventServiceImpl eventService;

    private EventEntity event;
    private ChurchEntity church;
    private UserEntity user;
    private UUID userId;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);

        userId = UUID.randomUUID();
        church = ChurchEntity.builder()
                .churchId(13L)
                .churchNumber("CH-001")
                .churchName("Test Church")
                .churchNameLocal("Test Church")
                .neighborhood("Neighborhood")
                .neighborhoodLocal("Neighborhood")
                .diocese("Diocese")
                .dioceseLocal("Diocese")
                .email("church@example.dev")
                .timezone("UTC")
                .usesOurServices(true)
                .status(ChurchStatus.DRAFT)
                .tenant(TenantEntity.builder().id(tenantId).build())
                .build();
        user = UserEntity.builder()
                .uuid(userId)
                .membership(Adult_MemberEntity.builder()
                        .churchId(church.getChurchId())
                        .build())
                .build();
        user.setTenantId(tenantId);
        Instant startAt = Instant.now().plusSeconds(3600);
        event = EventEntity.builder()
                .eventId(7L)
                .eventManagers(new HashSet<>())
                .tenantId(tenantId)
                .church(church)
                .timezone("UTC")
                .startAt(startAt)
                .endAt(startAt.plusSeconds(3600))
                .build();
        lenient().when(messageService.get(anyString(), anyString()))
                .thenAnswer(invocation -> invocation.getArgument(1));
        lenient().when(messageService.get(anyString(), anyString(), any(Object[].class)))
                .thenAnswer(invocation -> invocation.getArgument(1));
        lenient().when(eventRepository.findById(anyLong())).thenReturn(Optional.of(event));
        lenient().when(churchRepository.findById(anyLong())).thenReturn(Optional.of(church));
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
        manager.setUser(user);
        event.getEventManagers().add(manager);

        when(eventRepository.save(event)).thenReturn(event);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

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
        Instant newStart = Instant.now().plusSeconds(7200);
        EventEntity update = EventEntity.builder()
                .church(church)
                .timezone("UTC")
                .startAt(newStart)
                .endAt(newStart.plusSeconds(3600))
                .build();
        when(eventRepository.existsById(event.getEventId())).thenReturn(true);
        when(eventRepository.save(update)).thenAnswer(invocation -> invocation.getArgument(0));

        EventEntity result = eventService.updateEvent(event.getEventId(), update);

        assertThat(result.getEventId()).isEqualTo(event.getEventId());
        verify(eventRepository).save(update);
    }

    @Test
    void updateEvent_whenMissing_throwsException() {
        when(eventRepository.existsById(event.getEventId())).thenReturn(false);
        when(eventRepository.findById(event.getEventId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.updateEvent(event.getEventId(), EventEntity.builder().build()))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Event not found");
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
        when(eventRepository.findById(event.getEventId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.deleteEvent(event.getEventId()))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Event not found");
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

    @Test
    void visibleReadMethods_areTransactionalReadOnly() throws NoSuchMethodException {
        assertTransactionalReadOnly("getVisibleEventsForUser", UUID.class);
        assertTransactionalReadOnly("getEventByIdForUser", UUID.class, Long.class);
    }

    private void assertTransactionalReadOnly(String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
        Method method = EventServiceImpl.class.getMethod(methodName, parameterTypes);
        Transactional transactional = method.getAnnotation(Transactional.class);

        assertThat(transactional)
                .as("%s should be transactional", methodName)
                .isNotNull();
        assertThat(transactional.readOnly())
                .as("%s should be read-only transactional", methodName)
                .isTrue();
    }
}
