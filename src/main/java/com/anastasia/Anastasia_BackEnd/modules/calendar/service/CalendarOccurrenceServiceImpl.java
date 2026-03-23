package com.anastasia.Anastasia_BackEnd.modules.calendar.service;

import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.modules.calendar.dto.CalendarOccurrenceResponse;
import com.anastasia.Anastasia_BackEnd.modules.calendar.model.CalendarCategory;
import com.anastasia.Anastasia_BackEnd.modules.calendar.model.CalendarEntryAudienceEntity;
import com.anastasia.Anastasia_BackEnd.modules.calendar.model.CalendarEntryEntity;
import com.anastasia.Anastasia_BackEnd.modules.calendar.model.CalendarEntryStatus;
import com.anastasia.Anastasia_BackEnd.modules.calendar.model.CalendarEntryType;
import com.anastasia.Anastasia_BackEnd.modules.calendar.model.CalendarOccurrenceOverrideEntity;
import com.anastasia.Anastasia_BackEnd.modules.calendar.model.CalendarRecurrenceEntity;
import com.anastasia.Anastasia_BackEnd.modules.calendar.model.CalendarSystem;
import com.anastasia.Anastasia_BackEnd.modules.calendar.model.CalendarVisibility;
import com.anastasia.Anastasia_BackEnd.modules.calendar.model.RecurrenceFrequency;
import com.anastasia.Anastasia_BackEnd.modules.calendar.repository.CalendarEntryRepository;
import com.anastasia.Anastasia_BackEnd.modules.groups.model.GroupEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChurchRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CalendarOccurrenceServiceImpl implements CalendarOccurrenceService {

    private static final int MAX_OCCURRENCES_PER_ENTRY = 5000;
    private static final long MAX_RANGE_DAYS = 366;

    private final CalendarEntryRepository entryRepository;
    private final ChurchRepository churchRepository;
    private final UserRepository userRepository;
    private final GeezCalendarSupport geezCalendarSupport;

    @Override
    @Transactional(readOnly = true)
    public List<CalendarOccurrenceResponse> getOccurrences(
            Instant rangeStart,
            Instant rangeEnd,
            Set<CalendarEntryType> types,
            UUID userId,
            Set<String> authorities
    ) {
        if (rangeStart == null || rangeEnd == null) {
            throw new IllegalArgumentException("Both rangeStart and rangeEnd are required");
        }
        if (rangeEnd.isBefore(rangeStart)) {
            throw new IllegalArgumentException("rangeEnd must be after rangeStart");
        }
        if (Duration.between(rangeStart, rangeEnd).toDays() > MAX_RANGE_DAYS) {
            throw new IllegalArgumentException("Calendar occurrence range cannot exceed 366 days");
        }
        if (userId == null) {
            throw new IllegalArgumentException("User ID is required to resolve calendar visibility");
        }

        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant ID not found in context while fetching calendar entries");
        }

        ChurchEntity church = churchRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Church not found for tenant"));

        List<CalendarEntryEntity> entries;
        if (types == null || types.isEmpty()) {
            entries = entryRepository.findEntriesForRange(tenantId, church.getChurchId(), rangeStart, rangeEnd);
        } else {
            entries = entryRepository.findEntriesForRangeAndTypes(
                    tenantId,
                    church.getChurchId(),
                    rangeStart,
                    rangeEnd,
                    types
            );
        }

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        Set<Long> userGroupIds = resolveUserGroupIds(user);

        boolean staffAccess = hasStaffAccess(authorities);
        boolean priestAccess = hasPriestAccess(authorities);
        boolean allAccess = hasAllAccess(authorities);

        List<CalendarOccurrenceResponse> responses = new ArrayList<>();
        for (CalendarEntryEntity entry : entries) {
            if (!isVisibleToUser(entry, userId, userGroupIds, staffAccess, priestAccess, allAccess)) {
                continue;
            }
            responses.addAll(expandEntryOccurrences(entry, rangeStart, rangeEnd));
        }

        responses.sort(Comparator.comparing(CalendarOccurrenceResponse::startAtUtc));
        return responses;
    }

    private Set<Long> resolveUserGroupIds(UserEntity user) {
        Set<Long> groupIds = new HashSet<>();
        if (user == null || user.getGroups() == null) {
            return groupIds;
        }
        for (GroupEntity group : user.getGroups()) {
            if (group != null && group.getGroupId() != null) {
                groupIds.add(group.getGroupId());
            }
        }
        return groupIds;
    }

    private boolean isVisibleToUser(
            CalendarEntryEntity entry,
            UUID userId,
            Set<Long> userGroupIds,
            boolean staffAccess,
            boolean priestAccess,
            boolean allAccess
    ) {
        CalendarVisibility visibility = entry.getVisibility();
        if (visibility == null) {
            return false;
        }

        if (allAccess) {
            return true;
        }

        return switch (visibility) {
            case PUBLIC -> true;
            case PRIVATE -> userId.equals(entry.getOwnerUserId());
            case STAFF -> staffAccess || priestAccess;
            case PRIEST_ONLY -> priestAccess;
            case CUSTOM -> hasCustomAudience(entry.getAudiences(), userId, userGroupIds);
        };
    }

    private boolean hasCustomAudience(
            Collection<CalendarEntryAudienceEntity> audiences,
            UUID userId,
            Set<Long> userGroupIds
    ) {
        if (audiences == null || audiences.isEmpty()) {
            return false;
        }

        for (CalendarEntryAudienceEntity audience : audiences) {
            if (audience.getUser() != null && userId.equals(audience.getUser().getUuid())) {
                return true;
            }
            if (audience.getGroup() != null && userGroupIds.contains(audience.getGroup().getGroupId())) {
                return true;
            }
        }
        return false;
    }

    private boolean hasStaffAccess(Set<String> authorities) {
        return hasAnyAuthority(authorities, Set.of(
                "ROLE_OWNER",
                "ROLE_PRIEST",
                "ROLE_STAFF",
                "MANAGE_EVENTS",
                "VIEW_EVENTS",
                "MANAGE_APPOINTMENT",
                "VIEW_ALL_DATA",
                "MANAGE_SERVICES"
        ));
    }

    private boolean hasPriestAccess(Set<String> authorities) {
        return hasAnyAuthority(authorities, Set.of(
                "ROLE_OWNER",
                "ROLE_PRIEST",
                "VIEW_ALL_DATA"
        ));
    }

    private boolean hasAllAccess(Set<String> authorities) {
        return hasAnyAuthority(authorities, Set.of("ROLE_OWNER", "VIEW_ALL_DATA"));
    }

    private boolean hasAnyAuthority(Set<String> authorities, Set<String> expected) {
        if (authorities == null || authorities.isEmpty()) {
            return false;
        }
        for (String authority : authorities) {
            if (expected.contains(authority)) {
                return true;
            }
        }
        return false;
    }

    private List<CalendarOccurrenceResponse> expandEntryOccurrences(
            CalendarEntryEntity entry,
            Instant rangeStart,
            Instant rangeEnd
    ) {
        ZoneId zone = ZoneId.of(entry.getTimezone());
        LocalDate rangeStartDate = ZonedDateTime.ofInstant(rangeStart, zone).toLocalDate();
        LocalDate rangeEndDate = ZonedDateTime.ofInstant(rangeEnd, zone).toLocalDate();

        LocalDate baseDate = ZonedDateTime.ofInstant(entry.getStartAtUtc(), zone).toLocalDate();
        LocalTime baseStartTime = ZonedDateTime.ofInstant(entry.getStartAtUtc(), zone).toLocalTime();
        Duration duration = entry.getEndAtUtc() == null
                ? null
                : Duration.between(entry.getStartAtUtc(), entry.getEndAtUtc());

        List<LocalDate> dates = expandOccurrenceDates(
                entry.getRecurrence(),
                entry.getCalendarSystem(),
                baseDate,
                rangeStartDate,
                rangeEndDate,
                zone
        );

        Map<LocalDate, CalendarOccurrenceOverrideEntity> overrideMap = toOverrideMap(entry.getOverrides());
        List<CalendarOccurrenceResponse> responses = new ArrayList<>();

        for (LocalDate date : dates) {
            CalendarOccurrenceOverrideEntity override = overrideMap.get(date);
            boolean cancelled = entry.getStatus() == CalendarEntryStatus.CANCELED
                    || (override != null && override.isCancelled());

            String title = override != null && override.getTitleOverride() != null
                    ? override.getTitleOverride()
                    : entry.getTitle();

            Instant startAtUtc = override != null && override.getStartAtUtcOverride() != null
                    ? override.getStartAtUtcOverride()
                    : ZonedDateTime.of(date, baseStartTime, zone).toInstant();

            Instant endAtUtc = null;
            if (override != null && override.getEndAtUtcOverride() != null) {
                endAtUtc = override.getEndAtUtcOverride();
            } else if (duration != null) {
                endAtUtc = startAtUtc.plus(duration);
            }

            responses.add(new CalendarOccurrenceResponse(
                    entry.getId(),
                    entry.getType(),
                    title,
                    entry.getDescription(),
                    entry.getCalendarSystem(),
                    startAtUtc,
                    endAtUtc,
                    entry.getTimezone(),
                    entry.isAllDay(),
                    entry.getVisibility(),
                    entry.getStatus(),
                    entry.getSourceEntityType(),
                    entry.getSourceEntityId(),
                    date,
                    cancelled,
                    new HashSet<>(entry.getCategories()),
                    entry.getOwnerUserId()
            ));
        }

        return responses;
    }

    private Map<LocalDate, CalendarOccurrenceOverrideEntity> toOverrideMap(
            Collection<CalendarOccurrenceOverrideEntity> overrides
    ) {
        Map<LocalDate, CalendarOccurrenceOverrideEntity> map = new HashMap<>();
        if (overrides == null) {
            return map;
        }
        for (CalendarOccurrenceOverrideEntity override : overrides) {
            if (override.getOccurrenceDate() != null) {
                map.put(override.getOccurrenceDate(), override);
            }
        }
        return map;
    }

    private List<LocalDate> expandOccurrenceDates(
            CalendarRecurrenceEntity recurrence,
            CalendarSystem entrySystem,
            LocalDate baseDate,
            LocalDate rangeStart,
            LocalDate rangeEnd,
            ZoneId zone
    ) {
        if (recurrence == null || recurrence.getFrequency() == RecurrenceFrequency.NONE) {
            if (baseDate.isBefore(rangeStart) || baseDate.isAfter(rangeEnd)) {
                return List.of();
            }
            return List.of(baseDate);
        }

        RecurrenceFrequency frequency = recurrence.getFrequency();
        int interval = recurrence.getInterval() == null || recurrence.getInterval() < 1 ? 1 : recurrence.getInterval();

        if (recurrence.getCalendarSystem() == CalendarSystem.GEEZ && entrySystem == CalendarSystem.GEEZ) {
            return expandGeezOccurrences(recurrence, baseDate, rangeStart, rangeEnd, interval);
        }

        LocalDate effectiveEnd = rangeEnd;
        if (recurrence.getUntil() != null) {
            LocalDate untilDate = ZonedDateTime.ofInstant(recurrence.getUntil(), zone).toLocalDate();
            if (untilDate.isBefore(effectiveEnd)) {
                effectiveEnd = untilDate;
            }
        }

        return switch (frequency) {
            case DAILY -> expandDaily(baseDate, rangeStart, effectiveEnd, interval, recurrence.getCount());
            case WEEKLY -> expandWeekly(baseDate, rangeStart, effectiveEnd, interval, recurrence.getByDay(), recurrence.getCount());
            case MONTHLY -> expandMonthly(baseDate, rangeStart, effectiveEnd, interval, recurrence.getByMonth(), recurrence.getByMonthDay(), recurrence.getCount());
            case YEARLY -> expandYearly(baseDate, rangeStart, effectiveEnd, interval, recurrence.getByMonth(), recurrence.getByMonthDay(), recurrence.getCount());
            default -> List.of();
        };
    }

    private List<LocalDate> expandDaily(
            LocalDate baseDate,
            LocalDate rangeStart,
            LocalDate rangeEnd,
            int interval,
            Integer countLimit
    ) {
        List<LocalDate> results = new ArrayList<>();
        if (rangeEnd.isBefore(baseDate)) {
            return results;
        }

        LocalDate cursor = alignDate(baseDate, rangeStart, interval);
        int produced = countProducedBefore(baseDate, cursor, interval);

        while (!cursor.isAfter(rangeEnd) && withinCountLimit(produced, countLimit)) {
            if (!cursor.isBefore(rangeStart)) {
                results.add(cursor);
            }
            cursor = cursor.plusDays(interval);
            produced++;
            if (results.size() >= MAX_OCCURRENCES_PER_ENTRY) {
                break;
            }
        }
        return results;
    }

    private List<LocalDate> expandWeekly(
            LocalDate baseDate,
            LocalDate rangeStart,
            LocalDate rangeEnd,
            int interval,
            Set<DayOfWeek> byDay,
            Integer countLimit
    ) {
        List<LocalDate> results = new ArrayList<>();
        if (rangeEnd.isBefore(baseDate)) {
            return results;
        }

        Set<DayOfWeek> days = (byDay == null || byDay.isEmpty())
                ? EnumSet.of(baseDate.getDayOfWeek())
                : EnumSet.copyOf(byDay);

        LocalDate baseWeekStart = baseDate.with(DayOfWeek.MONDAY);
        long weeksBetween = ChronoUnit.WEEKS.between(baseWeekStart, rangeStart.with(DayOfWeek.MONDAY));
        long adjustedWeeks = (weeksBetween / interval) * interval;
        LocalDate cursorWeek = baseWeekStart.plusWeeks(adjustedWeeks);
        if (cursorWeek.isBefore(rangeStart.with(DayOfWeek.MONDAY))) {
            cursorWeek = cursorWeek.plusWeeks(interval);
        }

        int produced = 0;
        while (!cursorWeek.isAfter(rangeEnd) && withinCountLimit(produced, countLimit)) {
            for (DayOfWeek day : days) {
                LocalDate candidate = cursorWeek.plusDays(day.getValue() - DayOfWeek.MONDAY.getValue());
                if (candidate.isBefore(baseDate)) {
                    continue;
                }
                produced++;
                if (candidate.isBefore(rangeStart) || candidate.isAfter(rangeEnd)) {
                    continue;
                }
                results.add(candidate);
                if (results.size() >= MAX_OCCURRENCES_PER_ENTRY || !withinCountLimit(produced, countLimit)) {
                    break;
                }
            }
            cursorWeek = cursorWeek.plusWeeks(interval);
        }
        return results;
    }

    private List<LocalDate> expandMonthly(
            LocalDate baseDate,
            LocalDate rangeStart,
            LocalDate rangeEnd,
            int interval,
            Set<Integer> byMonth,
            Set<Integer> byMonthDay,
            Integer countLimit
    ) {
        List<LocalDate> results = new ArrayList<>();
        if (rangeEnd.isBefore(baseDate)) {
            return results;
        }

        Set<Integer> monthSet = (byMonth == null || byMonth.isEmpty())
                ? null
                : new HashSet<>(byMonth);
        Set<Integer> monthDays = (byMonthDay == null || byMonthDay.isEmpty())
                ? Set.of(baseDate.getDayOfMonth())
                : new HashSet<>(byMonthDay);

        YearMonth cursor = YearMonth.from(baseDate);
        YearMonth startMonth = YearMonth.from(rangeStart);
        if (cursor.isBefore(startMonth)) {
            long monthsBetween = ChronoUnit.MONTHS.between(cursor, startMonth);
            long adjusted = (monthsBetween / interval) * interval;
            cursor = cursor.plusMonths(adjusted);
            if (cursor.isBefore(startMonth)) {
                cursor = cursor.plusMonths(interval);
            }
        }

        int produced = 0;
        YearMonth endMonth = YearMonth.from(rangeEnd);
        while (!cursor.isAfter(endMonth) && withinCountLimit(produced, countLimit)) {
            if (monthSet == null || monthSet.contains(cursor.getMonthValue())) {
                for (Integer day : monthDays) {
                    if (day == null) {
                        continue;
                    }
                    if (!cursor.isValidDay(day)) {
                        continue;
                    }
                    LocalDate candidate = cursor.atDay(day);
                    if (candidate.isBefore(baseDate)) {
                        continue;
                    }
                    produced++;
                    if (candidate.isBefore(rangeStart) || candidate.isAfter(rangeEnd)) {
                        continue;
                    }
                    results.add(candidate);
                    if (results.size() >= MAX_OCCURRENCES_PER_ENTRY || !withinCountLimit(produced, countLimit)) {
                        break;
                    }
                }
            }
            cursor = cursor.plusMonths(interval);
        }
        return results;
    }

    private List<LocalDate> expandYearly(
            LocalDate baseDate,
            LocalDate rangeStart,
            LocalDate rangeEnd,
            int interval,
            Set<Integer> byMonth,
            Set<Integer> byMonthDay,
            Integer countLimit
    ) {
        List<LocalDate> results = new ArrayList<>();
        if (rangeEnd.isBefore(baseDate)) {
            return results;
        }

        Set<Integer> monthSet = (byMonth == null || byMonth.isEmpty())
                ? Set.of(baseDate.getMonthValue())
                : new HashSet<>(byMonth);
        Set<Integer> monthDays = (byMonthDay == null || byMonthDay.isEmpty())
                ? Set.of(baseDate.getDayOfMonth())
                : new HashSet<>(byMonthDay);

        int startYear = rangeStart.getYear();
        int endYear = rangeEnd.getYear();
        int year = baseDate.getYear();
        if (year < startYear) {
            int yearsBetween = startYear - year;
            int adjusted = (yearsBetween / interval) * interval;
            year = year + adjusted;
            if (year < startYear) {
                year += interval;
            }
        }

        int produced = 0;
        while (year <= endYear && withinCountLimit(produced, countLimit)) {
            for (Integer month : monthSet) {
                if (month == null || month < 1 || month > 12) {
                    continue;
                }
                YearMonth yearMonth = YearMonth.of(year, month);
                for (Integer day : monthDays) {
                    if (day == null || !yearMonth.isValidDay(day)) {
                        continue;
                    }
                    LocalDate candidate = yearMonth.atDay(day);
                    if (candidate.isBefore(baseDate)) {
                        continue;
                    }
                    produced++;
                    if (candidate.isBefore(rangeStart) || candidate.isAfter(rangeEnd)) {
                        continue;
                    }
                    results.add(candidate);
                    if (results.size() >= MAX_OCCURRENCES_PER_ENTRY || !withinCountLimit(produced, countLimit)) {
                        break;
                    }
                }
            }
            year += interval;
        }
        return results;
    }

    private LocalDate alignDate(LocalDate base, LocalDate target, int interval) {
        if (!base.isBefore(target)) {
            return base;
        }
        long daysBetween = ChronoUnit.DAYS.between(base, target);
        long steps = daysBetween / interval;
        LocalDate candidate = base.plusDays(steps * interval);
        if (candidate.isBefore(target)) {
            candidate = candidate.plusDays(interval);
        }
        return candidate;
    }

    private int countProducedBefore(LocalDate base, LocalDate cursor, int interval) {
        if (cursor.isBefore(base)) {
            return 0;
        }
        long daysBetween = ChronoUnit.DAYS.between(base, cursor);
        return (int) (daysBetween / interval);
    }

    private boolean withinCountLimit(int produced, Integer countLimit) {
        return countLimit == null || produced < countLimit;
    }

    private List<LocalDate> expandGeezOccurrences(
            CalendarRecurrenceEntity recurrence,
            LocalDate baseDate,
            LocalDate rangeStart,
            LocalDate rangeEnd,
            int interval
    ) {
        List<LocalDate> results = new ArrayList<>();
        if (rangeEnd.isBefore(baseDate)) {
            return results;
        }

        GeezCalendarSupport.GeezDate baseGeez = geezCalendarSupport.toGeezFromGregorian(baseDate);
        Integer geezMonth = recurrence.getGeezMonth();
        Integer geezDay = recurrence.getGeezDay();
        int produced = 0;

        LocalDate cursor = rangeStart.isAfter(baseDate) ? rangeStart : baseDate;
        while (!cursor.isAfter(rangeEnd) && withinCountLimit(produced, recurrence.getCount())) {
            GeezCalendarSupport.GeezDate candidate = geezCalendarSupport.toGeezFromGregorian(cursor);
            if (matchesGeezRecurrence(recurrence, baseGeez, candidate, geezMonth, geezDay, interval)) {
                results.add(cursor);
                produced++;
                if (results.size() >= MAX_OCCURRENCES_PER_ENTRY) {
                    break;
                }
            }
            cursor = cursor.plusDays(1);
        }

        return results;
    }

    private boolean matchesGeezRecurrence(
            CalendarRecurrenceEntity recurrence,
            GeezCalendarSupport.GeezDate baseGeez,
            GeezCalendarSupport.GeezDate candidate,
            Integer geezMonth,
            Integer geezDay,
            int interval
    ) {
        if (geezDay == null || geezDay < 1 || geezDay > 30) {
            return false;
        }

        return switch (recurrence.getFrequency()) {
            case MONTHLY -> candidate.day() == geezDay
                    && geezMonthsBetween(baseGeez, candidate) >= 0
                    && geezMonthsBetween(baseGeez, candidate) % interval == 0;
            case YEARLY -> geezMonth != null
                    && geezMonth >= 1
                    && geezMonth <= 13
                    && candidate.month() == geezMonth
                    && candidate.day() == geezDay
                    && candidate.year() >= baseGeez.year()
                    && (candidate.year() - baseGeez.year()) % interval == 0;
            default -> false;
        };
    }

    private int geezMonthsBetween(GeezCalendarSupport.GeezDate start, GeezCalendarSupport.GeezDate end) {
        return ((end.year() - start.year()) * 13) + (end.month() - start.month());
    }
}
