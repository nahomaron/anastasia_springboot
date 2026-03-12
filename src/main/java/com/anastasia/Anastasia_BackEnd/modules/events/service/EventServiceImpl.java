package com.anastasia.Anastasia_BackEnd.modules.events.service;

import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.common.i18n.LocalizedMessageService;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import com.anastasia.Anastasia_BackEnd.core.notification.channel.EmailNotificationService;
import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationChannelType;
import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationEvent;
import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationType;
import com.anastasia.Anastasia_BackEnd.core.notification.template.EmailTemplateName;
import com.anastasia.Anastasia_BackEnd.modules.events.mappers.EventManagerMapper;
import com.anastasia.Anastasia_BackEnd.modules.events.mappers.EventMapper;
import com.anastasia.Anastasia_BackEnd.modules.events.model.EventDTO;
import com.anastasia.Anastasia_BackEnd.modules.events.model.EventEntity;
import com.anastasia.Anastasia_BackEnd.modules.events.model.EventManagerEntity;
import com.anastasia.Anastasia_BackEnd.modules.events.model.EventStatus;
import com.anastasia.Anastasia_BackEnd.modules.events.model.requests.EventManagerDTO;
import com.anastasia.Anastasia_BackEnd.modules.events.repository.EventRepository;
import com.anastasia.Anastasia_BackEnd.modules.groups.GroupRepository;
import com.anastasia.Anastasia_BackEnd.modules.groups.model.GroupEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChurchRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.Adult_MemberEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private static final Pattern COORDINATE_PATTERN =
            Pattern.compile("(-?\\d{1,2}(?:\\.\\d+)?),\\s*(-?\\d{1,3}(?:\\.\\d+)?)");

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final EventMapper eventMapper;
    private final EventManagerMapper eventManagerMapper;
    private final GroupRepository groupRepository;
    private final ChurchRepository churchRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final EmailNotificationService emailNotificationService;
    private final LocalizedMessageService messageService;

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
    public List<EventDTO> getVisibleEventsForUser(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException(messageService.get(
                    "events.userId.required",
                    "User ID is required to resolve visible events"
            ));
        }

        UUID tenantId = requireTenantId();
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(messageService.get(
                        "user.notFound",
                        "User not found"
                )));

        return eventRepository.findVisibleForUser(tenantId, userId, user.getEmail()).stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Override
    public EventDTO getEventByIdForUser(UUID userId, Long eventId) {
        if (eventId == null) {
            throw new IllegalArgumentException(messageService.get(
                    "events.eventId.required",
                    "Event ID is required"
            ));
        }

        return getVisibleEventsForUser(userId).stream()
                .filter(event -> eventId.equals(event.getEventId()))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException(messageService.get(
                        "events.notFound",
                        "Event not found"
                )));
    }

    @Override
    public void assignManagerToEvent(Long eventId, UUID userId, String role) {
        UUID tenantId = requireTenantId();
        EventEntity event = findEventForTenant(eventId, tenantId);
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(messageService.get(
                        "user.notFound",
                        "User not found"
                )));

        assertUserInEventScope(event, user, "Manager");

        boolean exists = event.getEventManagers() != null && event.getEventManagers().stream()
                .map(EventManagerEntity::getUser)
                .filter(Objects::nonNull)
                .anyMatch(existing -> userId.equals(existing.getUuid()));
        if (exists) {
            return;
        }

        EventManagerEntity manager = new EventManagerEntity();
        manager.setEvent(event);
        manager.setUser(user);
        manager.setRole(role);

        if (event.getEventManagers() == null) {
            event.setEventManagers(new HashSet<>());
        }
        event.getEventManagers().add(manager);
        eventRepository.save(event);
    }

    @Override
    public void removeManager(Long eventId, UUID managerId) {
        UUID tenantId = requireTenantId();
        EventEntity event = findEventForTenant(eventId, tenantId);

        if (event.getEventManagers() == null || event.getEventManagers().isEmpty()) {
            throw new EntityNotFoundException(messageService.get(
                    "events.manager.notAssigned",
                    "Manager not assigned to event"
            ));
        }

        boolean removed = event.getEventManagers().removeIf(existingManager -> {
            UserEntity existingUser = existingManager.getUser();
            return existingUser != null && managerId.equals(existingUser.getUuid());
        });

        if (!removed) {
            throw new EntityNotFoundException(messageService.get(
                    "events.manager.notAssigned",
                    "Manager not assigned to event"
            ));
        }

        eventRepository.save(event);
    }

    @Override
    public List<EventManagerEntity> getManagers(Long eventId) {
        UUID tenantId = requireTenantId();
        findEventForTenant(eventId, tenantId);
        return eventRepository.findAllManagersByEventId(eventId);
    }

    @Caching(evict = {
            @CacheEvict(value = "events_visible", allEntries = true),
            @CacheEvict(value = "event_managers", allEntries = true)
    })
    @Override
    public EventEntity createEvent(EventEntity event) {
        UUID tenantId = requireTenantId();
        event.setTenantId(tenantId);

        normalizeAndValidateEvent(event, tenantId);
        EventEntity saved = eventRepository.save(event);
        notifyInvitees(saved, tenantId);
        return saved;
    }

    @Override
    public EventEntity updateEvent(Long eventId, EventEntity event) {
        UUID tenantId = requireTenantId();
        EventEntity existing = findEventForTenant(eventId, tenantId);

        event.setEventId(existing.getEventId());
        event.setTenantId(existing.getTenantId());

        normalizeAndValidateEvent(event, tenantId);
        EventEntity saved = eventRepository.save(event);
        notifyInvitees(saved, tenantId);
        return saved;
    }

    @Override
    public void deleteEvent(Long eventId) {
        UUID tenantId = requireTenantId();
        EventEntity event = findEventForTenant(eventId, tenantId);
        eventRepository.deleteById(event.getEventId());
    }

    private void normalizeAndValidateEvent(EventEntity event, UUID tenantId) {
        if (event.getChurch() == null || event.getChurch().getChurchId() == null) {
            throw new IllegalArgumentException(messageService.get(
                    "events.church.required",
                    "Church is required"
            ));
        }
        ChurchEntity resolvedChurch = churchRepository.findById(event.getChurch().getChurchId())
                .orElseThrow(() -> new EntityNotFoundException(messageService.get(
                        "church.notFound",
                        "Church not found"
                )));
        if (resolvedChurch.getTenant() == null || !tenantId.equals(resolvedChurch.getTenant().getId())) {
            throw new IllegalArgumentException(messageService.get(
                    "events.church.tenantMismatch",
                    "Church does not belong to current tenant"
            ));
        }
        event.setChurch(resolvedChurch);
        if (event.getTimezone() == null || event.getTimezone().isBlank()) {
            event.setTimezone(resolvedChurch.getTimezone());
        }
        if (event.getTimezone() == null || event.getTimezone().isBlank()) {
            event.setTimezone("UTC");
        }
        if (event.getStatus() == null) {
            event.setStatus(EventStatus.SCHEDULED);
        }

        normalizeInviteEmails(event);
        resolveInvitedEntities(event, tenantId);
        normalizeDateTimes(event);
        validateGeoSettings(event);
        validateManagersScope(event);
    }

    private void resolveInvitedEntities(EventEntity event, UUID tenantId) {
        if (event.getInvitedGroups() != null && !event.getInvitedGroups().isEmpty()) {
            Set<GroupEntity> resolvedGroups = new HashSet<>();
            for (GroupEntity incoming : event.getInvitedGroups()) {
                if (incoming == null || incoming.getGroupId() == null) {
                    continue;
                }
                GroupEntity resolved = groupRepository.findById(incoming.getGroupId())
                        .orElseThrow(() -> new EntityNotFoundException(messageService.get(
                                "groups.notFound.withId",
                                "Group not found: {0}",
                                incoming.getGroupId()
                        )));
                if (!tenantId.equals(resolved.getTenantId())) {
                    throw new IllegalArgumentException(messageService.get(
                            "groups.tenantMismatch",
                            "Group does not belong to current tenant"
                    ));
                }
                resolvedGroups.add(resolved);
            }
            event.setInvitedGroups(resolvedGroups);
        }

        if (event.getInvitedUsers() != null && !event.getInvitedUsers().isEmpty()) {
            Set<UserEntity> resolvedUsers = new HashSet<>();
            for (UserEntity incoming : event.getInvitedUsers()) {
                if (incoming == null || incoming.getUuid() == null) {
                    continue;
                }
                UserEntity resolved = userRepository.findById(incoming.getUuid())
                        .orElseThrow(() -> new EntityNotFoundException(messageService.get(
                                "user.notFound.withId",
                                "User not found: {0}",
                                incoming.getUuid()
                        )));
                assertUserInEventScope(event, resolved, "Invitee");
                resolvedUsers.add(resolved);
            }
            event.setInvitedUsers(resolvedUsers);
        }
    }

    private void normalizeInviteEmails(EventEntity event) {
        Set<String> normalized = new HashSet<>();
        Set<String> rawEmails = event.getInvitedEmails() == null ? Set.of() : event.getInvitedEmails();
        for (String email : rawEmails) {
            String cleaned = normalizeEmail(email);
            if (cleaned != null) {
                normalized.add(cleaned);
            }
        }
        event.setInvitedEmails(normalized);
    }

    private void normalizeDateTimes(EventEntity event) {
        LocalDateTime startAt = event.getStartAt();
        LocalDateTime endAt = event.getEndAt();

        if (event.isAllDay()) {
            if (startAt == null) {
                throw new IllegalArgumentException(messageService.get(
                        "events.start.required",
                        "startAt is required"
                ));
            }
            if (endAt == null) {
                endAt = startAt.plusDays(1);
            }
            startAt = startAt.toLocalDate().atStartOfDay();
            endAt = endAt.toLocalDate().plusDays(1).atStartOfDay();
            event.setStartAt(startAt);
            event.setEndAt(endAt);
            return;
        }

        if (startAt == null) {
            throw new IllegalArgumentException(messageService.get(
                    "events.start.required",
                    "startAt is required"
            ));
        }

        if (endAt != null && !endAt.isAfter(startAt)) {
            throw new IllegalArgumentException(messageService.get(
                    "events.end.afterStart",
                    "Event end must be after start"
            ));
        }
    }

    private void validateGeoSettings(EventEntity event) {
        boolean allowGeo = Boolean.TRUE.equals(event.getAllowGeoCheckIn());
        if (!allowGeo) {
            return;
        }

        String gps = event.getGpsLocation() == null ? "" : event.getGpsLocation().trim();
        if (gps.isBlank()) {
            throw new IllegalArgumentException(messageService.get(
                    "events.geo.gpsLocation.required",
                    "gpsLocation is required when geo check-in is enabled"
            ));
        }

        Double lat = event.getLatitude();
        Double lng = event.getLongitude();
        if (lat == null || lng == null) {
            double[] parsed = parseCoordinates(gps);
            if (parsed != null) {
                event.setLatitude(parsed[0]);
                event.setLongitude(parsed[1]);
                lat = parsed[0];
                lng = parsed[1];
            }
        }

        if (lat == null || lng == null) {
            throw new IllegalArgumentException(messageService.get(
                    "events.geo.coordinates.required",
                    "Latitude and longitude are required when geo check-in is enabled"
            ));
        }
        if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
            throw new IllegalArgumentException(messageService.get(
                    "events.geo.coordinates.invalid",
                    "Invalid latitude/longitude values"
            ));
        }
    }

    private void validateManagersScope(EventEntity event) {
        if (event.getEventManagers() == null || event.getEventManagers().isEmpty()) {
            return;
        }
        Set<EventManagerEntity> validManagers = new HashSet<>();
        for (EventManagerEntity manager : event.getEventManagers()) {
            if (manager == null || manager.getUser() == null || manager.getUser().getUuid() == null) {
                continue;
            }
            UserEntity persisted = userRepository.findById(manager.getUser().getUuid())
                    .orElseThrow(() -> new EntityNotFoundException(messageService.get(
                            "events.manager.user.notFound",
                            "Manager user not found"
                    )));
            assertUserInEventScope(event, persisted, "Manager");
            manager.setUser(persisted);
            manager.setEvent(event);
            validManagers.add(manager);
        }
        event.setEventManagers(validManagers);
    }

    private void notifyInvitees(EventEntity event, UUID tenantId) {
        Set<String> emailsToNotify = new HashSet<>(event.getInvitedEmails() == null ? Set.of() : event.getInvitedEmails());
        if (event.getInvitedUsers() != null) {
            event.getInvitedUsers().stream()
                    .map(UserEntity::getEmail)
                    .map(this::normalizeEmail)
                    .filter(Objects::nonNull)
                    .forEach(emailsToNotify::add);
        }

        for (String email : emailsToNotify) {
            notifyInviteeByEmail(event, tenantId, email);
        }
    }

    private void notifyInviteeByEmail(EventEntity event, UUID tenantId, String email) {
        Map<String, Object> props = invitationProperties(event, email);
        UserEntity scopedUser = userRepository.findByTenantIdAndEmailIgnoreCase(tenantId, email)
                .filter(user -> isUserInEventScope(event, user))
                .orElse(null);

        if (scopedUser != null) {
            eventPublisher.publishEvent(new NotificationEvent(
                    this,
                    NotificationType.NOTIFICATION,
                    scopedUser,
                    props,
                    EnumSet.of(NotificationChannelType.EMAIL, NotificationChannelType.IN_APP)
            ));
            return;
        }

        emailNotificationService.sendEmail(
                email,
                messageService.get(
                        "events.invitation.subject",
                        "You're invited: {0}",
                        event.getTitle()
                ),
                EmailTemplateName.NOTIFICATION,
                props
        );
    }

    private Map<String, Object> invitationProperties(EventEntity event, String email) {
        Map<String, Object> props = new HashMap<>();
        props.put("username", email);
        props.put("event_title", event.getTitle());
        props.put("event_location", event.getLocation());
        props.put("event_date", event.getStartAt() != null ? event.getStartAt().toLocalDate().toString() : "");
        props.put("event_start_at", event.getStartAt() != null ? event.getStartAt().toString() : "");
        props.put("message_content",
                messageService.get(
                        "events.invitation.message",
                        "You are invited to \"{0}\" at {1}.",
                        event.getTitle(),
                        event.getLocation()
                ));
        return props;
    }

    private void assertUserInEventScope(EventEntity event, UserEntity user, String subject) {
        if (!isUserInEventScope(event, user)) {
            throw new IllegalArgumentException(messageService.get(
                    "events.scope.user.invalid",
                    "{0} must belong to the same tenant/church membership scope",
                    subject
            ));
        }
    }

    private boolean isUserInEventScope(EventEntity event, UserEntity user) {
        if (user == null || user.getUuid() == null) {
            return false;
        }
        if (!Objects.equals(event.getTenantId(), user.getTenantId())) {
            return false;
        }
        Adult_MemberEntity membership = user.getMembership();
        Long userChurchId = membership != null ? membership.getChurchId() : null;
        Long eventChurchId = event.getChurch() != null ? event.getChurch().getChurchId() : null;
        return eventChurchId != null && Objects.equals(eventChurchId, userChurchId);
    }

    private EventEntity findEventForTenant(Long eventId, UUID tenantId) {
        EventEntity event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException(messageService.get(
                        "events.notFound",
                        "Event not found"
                )));
        if (!tenantId.equals(event.getTenantId())) {
            throw new EntityNotFoundException(messageService.get(
                    "events.notFound",
                    "Event not found"
            ));
        }
        return event;
    }

    private UUID requireTenantId() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException(messageService.get(
                    "tenant.context.missing",
                    "Tenant ID not found in context"
            ));
        }
        return tenantId;
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        return normalized.isBlank() ? null : normalized;
    }

    private double[] parseCoordinates(String source) {
        if (source == null || source.isBlank()) {
            return null;
        }
        Matcher matcher = COORDINATE_PATTERN.matcher(source);
        if (!matcher.find()) {
            return null;
        }
        double lat;
        double lng;
        try {
            lat = Double.parseDouble(matcher.group(1));
            lng = Double.parseDouble(matcher.group(2));
        } catch (NumberFormatException ex) {
            return null;
        }
        if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
            return null;
        }
        return new double[]{lat, lng};
    }
}
