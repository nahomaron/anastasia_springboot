package com.anastasia.Anastasia_BackEnd.modules.calendar.service;

import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import com.anastasia.Anastasia_BackEnd.modules.calendar.dto.CalendarEntryRequest;
import com.anastasia.Anastasia_BackEnd.modules.calendar.dto.CalendarEntryResponse;
import com.anastasia.Anastasia_BackEnd.modules.calendar.dto.CalendarRecurrenceRequest;
import com.anastasia.Anastasia_BackEnd.modules.calendar.dto.OccurrenceOverrideRequest;
import com.anastasia.Anastasia_BackEnd.modules.calendar.model.CalendarCategory;
import com.anastasia.Anastasia_BackEnd.modules.calendar.model.CalendarEntryAudienceEntity;
import com.anastasia.Anastasia_BackEnd.modules.calendar.model.CalendarEntryEntity;
import com.anastasia.Anastasia_BackEnd.modules.calendar.model.CalendarEntrySourceType;
import com.anastasia.Anastasia_BackEnd.modules.calendar.model.CalendarEntryStatus;
import com.anastasia.Anastasia_BackEnd.modules.calendar.model.CalendarOccurrenceOverrideEntity;
import com.anastasia.Anastasia_BackEnd.modules.calendar.model.CalendarRecurrenceEntity;
import com.anastasia.Anastasia_BackEnd.modules.calendar.model.CalendarVisibility;
import com.anastasia.Anastasia_BackEnd.modules.calendar.model.RecurrenceFrequency;
import com.anastasia.Anastasia_BackEnd.modules.calendar.repository.CalendarEntryRepository;
import com.anastasia.Anastasia_BackEnd.modules.groups.GroupRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChurchRepository;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CalendarEntryServiceImpl implements CalendarEntryService {

    private final CalendarEntryRepository entryRepository;
    private final ChurchRepository churchRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;

    @Override
    @Transactional
    public CalendarEntryResponse createEntry(CalendarEntryRequest request, UUID ownerUserId) {
        validateRequest(request);
        ChurchEntity church = resolveChurch();
        UserEntity owner = resolveUser(ownerUserId);

        CalendarEntryEntity entry = CalendarEntryEntity.builder()
                .tenantId(resolveTenantId())
                .church(church)
                .ownerUser(owner)
                .type(request.type())
                .title(request.title())
                .description(request.description())
                .calendarSystem(request.calendarSystem())
                .startAtUtc(request.startAtUtc())
                .endAtUtc(request.endAtUtc())
                .timezone(request.timezone())
                .allDay(request.allDay())
                .visibility(request.visibility())
                .status(CalendarEntryStatus.SCHEDULED)
                .statusChangedAt(Instant.now())
                .sourceEntityType(CalendarEntrySourceType.MANUAL)
                .categories(normalizeCategories(request.categories()))
                .build();

        applyRecurrence(entry, request.recurrence());
        applyAudience(entry, request.visibility(), request.audienceUserIds(), request.audienceGroupIds());

        return toResponse(entryRepository.save(entry));
    }

    @Override
    @Transactional
    public CalendarEntryResponse updateEntry(UUID entryId, CalendarEntryRequest request, UUID ownerUserId) {
        validateRequest(request);
        CalendarEntryEntity entry = resolveEntry(entryId);
        ensureTenant(entry);

        entry.setTitle(request.title());
        entry.setDescription(request.description());
        entry.setType(request.type());
        entry.setCalendarSystem(request.calendarSystem());
        entry.setStartAtUtc(request.startAtUtc());
        entry.setEndAtUtc(request.endAtUtc());
        entry.setTimezone(request.timezone());
        entry.setAllDay(request.allDay());
        entry.setVisibility(request.visibility());
        entry.setCategories(normalizeCategories(request.categories()));
        entry.setOwnerUser(resolveUser(ownerUserId));
        if (entry.getStatus() == null) {
            entry.setStatus(CalendarEntryStatus.SCHEDULED);
        }
        if (entry.getSourceEntityType() == null) {
            entry.setSourceEntityType(CalendarEntrySourceType.MANUAL);
        }

        applyRecurrence(entry, request.recurrence());
        applyAudience(entry, request.visibility(), request.audienceUserIds(), request.audienceGroupIds());

        return toResponse(entryRepository.save(entry));
    }

    @Override
    @Transactional
    public void applyOccurrenceOverride(UUID entryId, OccurrenceOverrideRequest request, UUID ownerUserId) {
        CalendarEntryEntity entry = resolveEntry(entryId);
        ensureTenant(entry);

        if (entry.getOwnerUserId() == null || !entry.getOwnerUserId().equals(ownerUserId)) {
            throw new IllegalStateException("Only the entry owner can override an occurrence");
        }

        CalendarOccurrenceOverrideEntity override = entry.getOverrides().stream()
                .filter(existing -> existing.getOccurrenceDate().equals(request.occurrenceDate()))
                .findFirst()
                .orElseGet(() -> CalendarOccurrenceOverrideEntity.builder()
                        .entry(entry)
                        .occurrenceDate(request.occurrenceDate())
                        .build());

        override.setCancelled(request.cancelled());
        override.setTitleOverride(request.titleOverride());
        override.setStartAtUtcOverride(request.startAtUtcOverride());
        override.setEndAtUtcOverride(request.endAtUtcOverride());
        override.setNotes(request.notes());

        if (override.getStartAtUtcOverride() != null && override.getEndAtUtcOverride() != null
                && override.getEndAtUtcOverride().isBefore(override.getStartAtUtcOverride())) {
            throw new IllegalArgumentException("endAtUtcOverride must be after startAtUtcOverride");
        }

        entry.getOverrides().add(override);
        entryRepository.save(entry);
    }

    @Override
    @Transactional
    public CalendarEntryResponse splitSeries(
            UUID entryId,
            LocalDate occurrenceDate,
            CalendarEntryRequest newSeriesRequest,
            UUID ownerUserId
    ) {
        if (occurrenceDate == null) {
            throw new IllegalArgumentException("occurrenceDate is required");
        }
        validateRequest(newSeriesRequest);

        CalendarEntryEntity entry = resolveEntry(entryId);
        ensureTenant(entry);

        if (entry.getRecurrence() == null || entry.getRecurrence().getFrequency() == RecurrenceFrequency.NONE) {
            throw new IllegalStateException("THIS_AND_FUTURE requires a recurring series");
        }

        if (entry.getOwnerUserId() == null || !entry.getOwnerUserId().equals(ownerUserId)) {
            throw new IllegalStateException("Only the entry owner can split a series");
        }

        ZoneId zone = ZoneId.of(entry.getTimezone());
        ZonedDateTime occurrenceStart = occurrenceDate.atStartOfDay(zone);
        ZonedDateTime priorDayEnd = occurrenceStart.minusSeconds(1);

        CalendarRecurrenceEntity recurrence = entry.getRecurrence();
        recurrence.setUntil(priorDayEnd.toInstant());
        entry.setRecurrence(recurrence);
        entryRepository.save(entry);

        ZonedDateTime newSeriesStart = ZonedDateTime.ofInstant(newSeriesRequest.startAtUtc(), ZoneId.of(newSeriesRequest.timezone()));
        if (!newSeriesStart.toLocalDate().equals(occurrenceDate)) {
            throw new IllegalArgumentException("new series startAtUtc must match occurrenceDate");
        }

        return createEntry(newSeriesRequest, ownerUserId);
    }

    private void validateRequest(CalendarEntryRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Calendar entry payload is required");
        }
        if (request.startAtUtc() == null) {
            throw new IllegalArgumentException("startAtUtc is required");
        }
        if (request.endAtUtc() != null && request.endAtUtc().isBefore(request.startAtUtc())) {
            throw new IllegalArgumentException("endAtUtc must be after startAtUtc");
        }
        ZoneId.of(request.timezone());
        validateRecurrence(request);
    }

    private ChurchEntity resolveChurch() {
        UUID tenantId = resolveTenantId();
        return churchRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Church not found for tenant"));
    }

    private CalendarEntryEntity resolveEntry(UUID entryId) {
        return entryRepository.findById(entryId)
                .orElseThrow(() -> new EntityNotFoundException("Calendar entry not found"));
    }

    private void ensureTenant(CalendarEntryEntity entry) {
        UUID tenantId = resolveTenantId();
        if (!tenantId.equals(entry.getTenantId())) {
            throw new IllegalStateException("Calendar entry does not belong to current tenant");
        }
    }

    private UUID resolveTenantId() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant ID not found in context");
        }
        return tenantId;
    }

    private UserEntity resolveUser(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID is required");
        }
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        UUID tenantId = resolveTenantId();
        if (!tenantId.equals(user.getTenantId())) {
            throw new IllegalStateException("User does not belong to current tenant");
        }
        return user;
    }

    private Set<CalendarCategory> normalizeCategories(Set<CalendarCategory> categories) {
        return categories == null ? new HashSet<>() : new HashSet<>(categories);
    }

    private void applyRecurrence(CalendarEntryEntity entry, CalendarRecurrenceRequest request) {
        if (request == null || request.frequency() == null || request.frequency() == RecurrenceFrequency.NONE) {
            entry.setRecurrence(null);
            return;
        }

        CalendarRecurrenceEntity recurrence = Optional.ofNullable(entry.getRecurrence())
                .orElseGet(CalendarRecurrenceEntity::new);

        recurrence.setEntry(entry);
        recurrence.setFrequency(request.frequency());
        recurrence.setInterval(request.interval());
        recurrence.setByDay(request.byDay() == null ? new HashSet<>() : new HashSet<>(request.byDay()));
        recurrence.setByMonth(request.byMonth() == null ? new HashSet<>() : new HashSet<>(request.byMonth()));
        recurrence.setByMonthDay(request.byMonthDay() == null ? new HashSet<>() : new HashSet<>(request.byMonthDay()));
        recurrence.setUntil(request.until());
        recurrence.setCount(request.count());
        recurrence.setCalendarSystem(request.calendarSystem());
        recurrence.setGeezMonth(request.geezMonth());
        recurrence.setGeezDay(request.geezDay());

        entry.setRecurrence(recurrence);
    }

    private void applyAudience(
            CalendarEntryEntity entry,
            CalendarVisibility visibility,
            Set<UUID> audienceUserIds,
            Set<Long> audienceGroupIds
    ) {
        entry.getAudiences().clear();
        if (visibility != CalendarVisibility.CUSTOM) {
            return;
        }

        Set<UUID> userIds = audienceUserIds == null ? Set.of() : audienceUserIds;
        Set<Long> groupIds = audienceGroupIds == null ? Set.of() : audienceGroupIds;
        if (userIds.isEmpty() && groupIds.isEmpty()) {
            throw new IllegalArgumentException("Custom visibility requires at least one audience user or group");
        }
        UUID tenantId = resolveTenantId();

        if (!userIds.isEmpty()) {
            userRepository.findAllById(userIds).forEach(user -> {
                if (!tenantId.equals(user.getTenantId())) {
                    throw new IllegalStateException("Audience user does not belong to current tenant");
                }
                CalendarEntryAudienceEntity audience = CalendarEntryAudienceEntity.builder()
                        .entry(entry)
                        .user(user)
                        .build();
                entry.getAudiences().add(audience);
            });
        }

        if (!groupIds.isEmpty()) {
            groupRepository.findAllById(groupIds).forEach(group -> {
                if (!tenantId.equals(group.getTenantId())) {
                    throw new IllegalStateException("Audience group does not belong to current tenant");
                }
                CalendarEntryAudienceEntity audience = CalendarEntryAudienceEntity.builder()
                        .entry(entry)
                        .group(group)
                        .build();
                entry.getAudiences().add(audience);
            });
        }
    }

    private CalendarEntryResponse toResponse(CalendarEntryEntity entry) {
        return new CalendarEntryResponse(
                entry.getId(),
                entry.getType(),
                entry.getTitle(),
                entry.getDescription(),
                entry.getCalendarSystem(),
                entry.getStartAtUtc(),
                entry.getEndAtUtc(),
                entry.getTimezone(),
                entry.isAllDay(),
                entry.getVisibility(),
                entry.getStatus(),
                entry.getSourceEntityType(),
                entry.getSourceEntityId(),
                normalizeCategories(entry.getCategories()),
                entry.getOwnerUserId()
        );
    }

    private void validateRecurrence(CalendarEntryRequest request) {
        CalendarRecurrenceRequest recurrence = request.recurrence();
        if (recurrence == null || recurrence.frequency() == null || recurrence.frequency() == RecurrenceFrequency.NONE) {
            return;
        }
        if (recurrence.interval() != null && recurrence.interval() < 1) {
            throw new IllegalArgumentException("recurrence interval must be at least 1");
        }
        if (recurrence.count() != null && recurrence.count() < 1) {
            throw new IllegalArgumentException("recurrence count must be at least 1");
        }
        if (recurrence.until() != null && recurrence.until().isBefore(request.startAtUtc())) {
            throw new IllegalArgumentException("recurrence until must be after startAtUtc");
        }
        if (recurrence.frequency() == RecurrenceFrequency.WEEKLY
                && (recurrence.byDay() == null || recurrence.byDay().isEmpty())) {
            throw new IllegalArgumentException("weekly recurrence requires at least one weekday");
        }
        if (recurrence.byMonth() != null && recurrence.byMonth().stream().anyMatch(month -> month == null || month < 1 || month > 12)) {
            throw new IllegalArgumentException("recurrence byMonth values must be between 1 and 12");
        }
        if (recurrence.byMonthDay() != null
                && recurrence.byMonthDay().stream().anyMatch(day -> day == null || day < 1 || day > 31)) {
            throw new IllegalArgumentException("recurrence byMonthDay values must be between 1 and 31");
        }
    }
}
