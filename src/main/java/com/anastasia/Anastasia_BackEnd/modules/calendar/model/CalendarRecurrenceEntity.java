package com.anastasia.Anastasia_BackEnd.modules.calendar.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "calendar_recurrence")
public class CalendarRecurrenceEntity {

    @Id
    private UUID entryId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "entry_id")
    private CalendarEntryEntity entry;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RecurrenceFrequency frequency = RecurrenceFrequency.NONE;

    @Column(name = "interval_value")
    @Builder.Default
    private Integer interval = 1;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "calendar_recurrence_by_day", joinColumns = @JoinColumn(name = "entry_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "weekday", nullable = false)
    @Builder.Default
    private Set<DayOfWeek> byDay = new HashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "calendar_recurrence_by_month", joinColumns = @JoinColumn(name = "entry_id"))
    @Column(name = "occurrence_month", nullable = false)
    @Builder.Default
    private Set<Integer> byMonth = new HashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "calendar_recurrence_by_month_day", joinColumns = @JoinColumn(name = "entry_id"))
    @Column(name = "month_day", nullable = false)
    @Builder.Default
    private Set<Integer> byMonthDay = new HashSet<>();

    @Column(name = "until_at")
    private Instant until;

    private Integer count;

    @Enumerated(EnumType.STRING)
    @Column(name = "calendar_system")
    private CalendarSystem calendarSystem;

    @Column(name = "geez_month")
    private Integer geezMonth;

    @Column(name = "geez_day")
    private Integer geezDay;
}
