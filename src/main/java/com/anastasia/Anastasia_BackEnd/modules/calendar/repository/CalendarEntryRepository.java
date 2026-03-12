package com.anastasia.Anastasia_BackEnd.modules.calendar.repository;

import com.anastasia.Anastasia_BackEnd.modules.calendar.model.CalendarEntryEntity;
import com.anastasia.Anastasia_BackEnd.modules.calendar.model.CalendarEntryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public interface CalendarEntryRepository extends JpaRepository<CalendarEntryEntity, UUID> {

    @Query("""
        select distinct e from CalendarEntryEntity e
        left join fetch e.recurrence r
        left join fetch e.overrides o
        left join fetch e.audiences a
        left join fetch a.user
        left join fetch a.group
        where e.tenantId = :tenantId
          and e.church.churchId = :churchId
          and e.deletedAt is null
          and e.status <> com.anastasia.Anastasia_BackEnd.modules.calendar.model.CalendarEntryStatus.ARCHIVED
          and (
                (r is null and e.startAtUtc <= :rangeEnd and (e.endAtUtc is null or e.endAtUtc >= :rangeStart))
                or
                (r is not null and e.startAtUtc <= :rangeEnd and (r.until is null or r.until >= :rangeStart))
          )
    """)
    List<CalendarEntryEntity> findEntriesForRange(
            @Param("tenantId") UUID tenantId,
            @Param("churchId") Long churchId,
            @Param("rangeStart") Instant rangeStart,
            @Param("rangeEnd") Instant rangeEnd
    );

    @Query("""
        select distinct e from CalendarEntryEntity e
        left join fetch e.recurrence r
        left join fetch e.overrides o
        left join fetch e.audiences a
        left join fetch a.user
        left join fetch a.group
        where e.tenantId = :tenantId
          and e.church.churchId = :churchId
          and e.type in :types
          and e.deletedAt is null
          and e.status <> com.anastasia.Anastasia_BackEnd.modules.calendar.model.CalendarEntryStatus.ARCHIVED
          and (
                (r is null and e.startAtUtc <= :rangeEnd and (e.endAtUtc is null or e.endAtUtc >= :rangeStart))
                or
                (r is not null and e.startAtUtc <= :rangeEnd and (r.until is null or r.until >= :rangeStart))
          )
    """)
    List<CalendarEntryEntity> findEntriesForRangeAndTypes(
            @Param("tenantId") UUID tenantId,
            @Param("churchId") Long churchId,
            @Param("rangeStart") Instant rangeStart,
            @Param("rangeEnd") Instant rangeEnd,
            @Param("types") Set<CalendarEntryType> types
    );
}
